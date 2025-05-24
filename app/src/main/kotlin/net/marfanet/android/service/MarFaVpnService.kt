package net.marfanet.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.marfanet.android.MainActivity
import net.marfanet.android.R
import net.marfanet.android.data.ProfileEntity
import net.marfanet.android.logging.ConnectionLogger
import net.marfanet.android.stats.VpnStatsCollector
import net.marfanet.android.xray.XrayConfigBuilder
import net.marfanet.android.xray.XrayCore
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Connection information data class
 */
data class ConnectionInfo(
    val state: VpnConnectionState,
    val profileName: String? = null,
    val serverAddress: String? = null,
    val protocol: String? = null,
    val latency: Long? = null,
    val connectedAt: Long? = null,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0
)

/**
 * MarFa VPN Service
 * Main VPN service that manages Xray core and network tunnel
 */
@AndroidEntryPoint
class MarFaVpnService : VpnService() {
    
    companion object {
        private const val TAG = "MarFaVpnService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "marfa_vpn_channel"
        private const val ACTION_CONNECT = "net.marfanet.android.CONNECT"
        private const val ACTION_DISCONNECT = "net.marfanet.android.DISCONNECT"
        private const val ACTION_SMART_CONNECT = "net.marfanet.android.SMART_CONNECT"
        
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val VPN_MTU = 1500
        private const val STATS_UPDATE_INTERVAL = 1000L
    }
    
    @Inject
    lateinit var xrayCore: XrayCore
    
    @Inject
    lateinit var configBuilder: XrayConfigBuilder
    
    @Inject
    lateinit var connectionLogger: ConnectionLogger
    
    @Inject
    lateinit var statsCollector: VpnStatsCollector
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private var statsJob: Job? = null
    private var currentProfile: ProfileEntity? = null
    
    private val _connectionState = MutableStateFlow(VpnConnectionState.DISCONNECTED)
    val connectionState: StateFlow<VpnConnectionState> = _connectionState
    
    private val _connectionInfo = MutableStateFlow(ConnectionInfo(VpnConnectionState.DISCONNECTED))
    val connectionInfo: StateFlow<ConnectionInfo> = _connectionInfo
    
    private var startTime: Long = 0
    private var lastRxBytes: Long = 0
    private var lastTxBytes: Long = 0
    
    private val binder = VpnServiceBinder()
    
    inner class VpnServiceBinder : Binder() {
        fun getService(): MarFaVpnService = this@MarFaVpnService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VPN Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) {
                    // In real implementation, fetch profile from database
                    // For now, create a test profile
                    val testProfile = createTestProfile(profileId)
                    connect(testProfile)
                }
            }
            ACTION_SMART_CONNECT -> {
                smartConnect()
            }
            ACTION_DISCONNECT -> {
                disconnect()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VPN Service destroyed")
        disconnect()
    }
    
    /**
     * Smart connect - automatically selects the best available server
     */
    private fun smartConnect() {
        serviceScope.launch {
            try {
                Log.i(TAG, "Starting smart connect...")
                updateConnectionInfo(VpnConnectionState.CONNECTING, "Finding best server...")
                
                // TODO: In real implementation, get profiles from database and ping them
                // For now, create a test profile with simulated best latency
                val bestProfile = createTestProfile("smart-selected").copy(
                    name = "Smart Selected Server",
                    latency = 45L // Simulated low latency
                )
                
                Log.d(TAG, "Smart connect selected: ${bestProfile.name} (${bestProfile.latency}ms)")
                connect(bestProfile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Smart connect failed", e)
                updateConnectionInfo(VpnConnectionState.ERROR, "Smart connect failed")
            }
        }
    }
    
    /**
     * Connect to VPN using the specified profile
     */
    fun connect(profile: ProfileEntity) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Connecting to VPN with profile: ${profile.name}")
                updateConnectionInfo(VpnConnectionState.CONNECTING, "Initializing...")
                
                // Log connection attempt
                connectionLogger.logConnectionAttempt(
                    serverId = profile.id,
                    protocol = profile.protocol,
                    serverAddress = profile.serverAddress,
                    serverPort = profile.serverPort
                )
                
                // Build Xray configuration
                updateConnectionInfo(VpnConnectionState.CONNECTING, "Configuring proxy...")
                val xrayConfig = configBuilder.buildConfig(
                    profile = profile,
                    enableGfwRules = true,
                    bypassApps = emptyList(), // TODO: Get from settings
                    customRules = emptyList() // TODO: Get from settings
                )
                
                // Start Xray core
                updateConnectionInfo(VpnConnectionState.CONNECTING, "Starting proxy core...")
                val xrayStarted = xrayCore.start(xrayConfig)
                if (!xrayStarted) {
                    throw Exception("Failed to start Xray core")
                }
                
                // Request VPN permission if needed
                val intent = prepare(this@MarFaVpnService)
                if (intent != null) {
                    Log.w(TAG, "VPN permission not granted")
                    updateConnectionInfo(VpnConnectionState.PERMISSION_REQUIRED, "Permission required")
                    return@launch
                }
                
                // Create VPN interface
                updateConnectionInfo(VpnConnectionState.CONNECTING, "Establishing tunnel...")
                val vpnBuilder = Builder()
                    .setSession("MarFaNet VPN - ${profile.name}")
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("208.67.222.222") // OpenDNS
                    .setMtu(VPN_MTU)
                    .setBlocking(false)
                
                // Establish VPN interface
                vpnInterface = vpnBuilder.establish()
                if (vpnInterface == null) {
                    throw Exception("Failed to establish VPN interface")
                }
                
                currentProfile = profile
                startTime = System.currentTimeMillis()
                
                // Start foreground notification
                startForeground(NOTIFICATION_ID, createNotification(profile))
                
                // Start packet forwarding with real traffic monitoring
                startRealPacketForwarding()
                
                // Start statistics collection
                startStatsCollection()
                statsCollector.startCollecting()
                
                // Update connection info with full details
                updateConnectionInfo(
                    VpnConnectionState.CONNECTED,
                    profile.name,
                    profile.serverAddress,
                    profile.protocol,
                    profile.latency,
                    startTime
                )
                
                // Log successful connection
                connectionLogger.logConnectionSuccess(
                    serverId = profile.id,
                    protocol = profile.protocol,
                    connectionTimeMs = System.currentTimeMillis() - startTime,
                    selectedServer = profile.name
                )
                
                Log.i(TAG, "VPN connected successfully to ${profile.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect VPN", e)
                updateConnectionInfo(VpnConnectionState.ERROR, "Connection failed: ${e.message}")
                
                // Log connection failure
                connectionLogger.logConnectionFailure(
                    serverId = profile.id,
                    protocol = profile.protocol,
                    errorCode = "connection_failed",
                    errorMessage = e.message ?: "Unknown error",
                    retryAttempt = 1
                )
                
                // Cleanup on failure
                cleanup()
            }
        }
    }
    
    /**
     * Disconnect from VPN
     */
    fun disconnect() {
        serviceScope.launch {
            try {
                Log.i(TAG, "Disconnecting VPN")
                updateConnectionInfo(VpnConnectionState.DISCONNECTING, "Disconnecting...")
                
                val profile = currentProfile
                if (profile != null) {
                    val sessionDuration = if (startTime > 0) {
                        System.currentTimeMillis() - startTime
                    } else {
                        0L
                    }
                    
                    connectionLogger.logDisconnection(
                        serverId = profile.id,
                        protocol = profile.protocol,
                        sessionDurationMs = sessionDuration,
                        disconnectReason = "user_disconnect",
                        isUserInitiated = true
                    )
                }
                
                cleanup()
                updateConnectionInfo(VpnConnectionState.DISCONNECTED)
                
                Log.i(TAG, "VPN disconnected")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during disconnect", e)
                updateConnectionInfo(VpnConnectionState.ERROR, "Disconnect failed")
            }
        }
    }
    
    private fun cleanup() {
        // Stop packet forwarding
        tunnelJob?.cancel()
        tunnelJob = null
        
        // Stop statistics collection
        statsJob?.cancel()
        statsJob = null
        statsCollector.stopCollecting()
        
        // Stop Xray core
        serviceScope.launch {
            xrayCore.stop()
        }
        
        // Close VPN interface
        vpnInterface?.close()
        vpnInterface = null
        
        // Reset stats
        lastRxBytes = 0
        lastTxBytes = 0
        startTime = 0
        currentProfile = null
        
        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    private fun startRealPacketForwarding() {
        val vpnFd = vpnInterface ?: return
        
        tunnelJob = serviceScope.launch {
            try {
                Log.d(TAG, "Starting real packet forwarding with Xray integration")
                
                val inputStream = FileInputStream(vpnFd.fileDescriptor)
                val outputStream = FileOutputStream(vpnFd.fileDescriptor)
                val buffer = ByteBuffer.allocate(VPN_MTU)
                
                while (tunnelJob?.isActive == true) {
                    try {
                        // Read packet from TUN interface
                        val length = inputStream.read(buffer.array())
                        if (length > 0) {
                            // Forward packet to Xray core for processing
                            val processed = xrayCore.processPacket(buffer.array(), length)
                            if (processed != null && processed.isNotEmpty()) {
                                outputStream.write(processed)
                                lastTxBytes += processed.size
                            }
                            lastRxBytes += length
                        }
                        
                        // Handle outgoing packets from Xray
                        val outgoingData = xrayCore.getOutgoingPackets()
                        if (outgoingData.isNotEmpty()) {
                            outputStream.write(outgoingData)
                            lastTxBytes += outgoingData.size
                        }
                        
                    } catch (e: Exception) {
                        if (tunnelJob?.isActive == true) {
                            Log.w(TAG, "Packet processing error: ${e.message}")
                        }
                    }
                    
                    // Small delay to prevent busy waiting
                    kotlinx.coroutines.delay(1)
                }
                
            } catch (e: Exception) {
                if (tunnelJob?.isActive == true) {
                    Log.e(TAG, "Critical error in packet forwarding", e)
                }
            }
        }
    }
    
    private fun startStatsCollection() {
        statsJob = serviceScope.launch {
            while (statsJob?.isActive == true && _connectionInfo.value.state == VpnConnectionState.CONNECTED) {
                try {
                    val currentInfo = _connectionInfo.value
                    val updatedInfo = currentInfo.copy(
                        bytesReceived = lastRxBytes,
                        bytesSent = lastTxBytes
                    )
                    _connectionInfo.value = updatedInfo
                    
                    kotlinx.coroutines.delay(STATS_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Stats collection error", e)
                    break
                }
            }
        }
    }
    
    private fun updateConnectionInfo(
        state: VpnConnectionState,
        profileName: String? = null,
        serverAddress: String? = null,
        protocol: String? = null,
        latency: Long? = null,
        connectedAt: Long? = null
    ) {
        val info = ConnectionInfo(
            state = state,
            profileName = profileName,
            serverAddress = serverAddress,
            protocol = protocol,
            latency = latency,
            connectedAt = connectedAt,
            bytesReceived = _connectionInfo.value.bytesReceived,
            bytesSent = _connectionInfo.value.bytesSent
        )
        _connectionInfo.value = info
        _connectionState.value = state
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MarFa VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "MarFa VPN connection status"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(profile: ProfileEntity): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val disconnectIntent = Intent(this, MarFaVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 0, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MarFa VPN Connected")
            .setContentText("Connected to ${profile.name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Disconnect",
                disconnectPendingIntent
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun createTestProfile(profileId: String): ProfileEntity {
        // Create a test profile for development
        return ProfileEntity(
            id = profileId,
            name = "Test Server",
            protocol = "vmess",
            serverAddress = "example.com",
            serverPort = 443,
            userId = "test-user-id",
            password = "",
            security = "auto",
            network = "ws",
            path = "/",
            host = "example.com",
            tls = true,
            sni = "example.com",
            allowInsecure = false,
            latency = 85L // Simulated latency
        )
    }
    
    fun getCurrentProfile(): ProfileEntity? = currentProfile
    
    fun getConnectionState(): VpnConnectionState = _connectionState.value
    
    fun getConnectionInfo(): ConnectionInfo = _connectionInfo.value
}

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR,
    PERMISSION_REQUIRED
}

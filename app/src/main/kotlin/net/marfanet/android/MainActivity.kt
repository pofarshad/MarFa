package net.marfanet.android

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.marfanet.android.service.ConnectionInfo
import net.marfanet.android.service.MarFaVpnService
import net.marfanet.android.service.PingService
import net.marfanet.android.service.VpnConnectionState
import net.marfanet.android.subscription.SubscriptionParser
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val VPN_REQUEST_CODE = 1001
    }
    
    @Inject
    lateinit var pingService: PingService
    
    @Inject
    lateinit var subscriptionParser: SubscriptionParser
    
    private var vpnService: MarFaVpnService? = null
    private var isBound = false
    private var connectionInfo by mutableStateOf(ConnectionInfo(VpnConnectionState.DISCONNECTED))
    
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "VPN permission granted")
            connectToVpn()
        } else {
            Log.w(TAG, "VPN permission denied")
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MarFaVpnService.VpnServiceBinder
            vpnService = binder.getService()
            isBound = true
            Log.d(TAG, "VPN service connected")
            
            // Start monitoring connection info
            startConnectionInfoMonitoring()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            vpnService = null
            isBound = false
            Log.d(TAG, "VPN service disconnected")
        }
    }
    
    private fun startConnectionInfoMonitoring() {
        lifecycleScope.launch {
            while (isBound && vpnService != null) {
                try {
                    connectionInfo = vpnService!!.getConnectionInfo()
                    delay(1000) // Update every second
                } catch (e: Exception) {
                    Log.e(TAG, "Error monitoring connection info", e)
                    delay(5000) // Retry after 5 seconds on error
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bind to VPN service
        val intent = Intent(this, MarFaVpnService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        setContent {
            MarFaNetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MarFaNetMainScreen(
                        connectionInfo = connectionInfo,
                        onConnectClick = { handleConnectClick() },
                        onSmartConnectClick = { handleSmartConnectClick() },
                        onDisconnectClick = { handleDisconnectClick() }
                    )
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
    
    private fun handleConnectClick() {
        // Check VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            connectToVpn()
        }
    }
    
    private fun handleSmartConnectClick() {
        // Check VPN permission
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            smartConnectToVpn()
        }
    }
    
    private fun handleDisconnectClick() {
        vpnService?.disconnect()
    }
    
    private fun connectToVpn() {
        lifecycleScope.launch {
            try {
                // Start VPN service with regular connect
                val intent = Intent(this@MainActivity, MarFaVpnService::class.java).apply {
                    action = "net.marfanet.android.CONNECT"
                    putExtra(MarFaVpnService.EXTRA_PROFILE_ID, "test-profile")
                }
                startService(intent)
                
                Log.d(TAG, "VPN connection initiated")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VPN", e)
            }
        }
    }
    
    private fun smartConnectToVpn() {
        lifecycleScope.launch {
            try {
                // Start ping monitoring if not already started
                pingService.startPingMonitoring()
                
                // Start VPN service with smart connect
                val intent = Intent(this@MainActivity, MarFaVpnService::class.java).apply {
                    action = "net.marfanet.android.SMART_CONNECT"
                }
                startService(intent)
                
                Log.d(TAG, "Smart VPN connection initiated")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start smart VPN", e)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarFaNetMainScreen(
    connectionInfo: ConnectionInfo,
    onConnectClick: () -> Unit = {},
    onSmartConnectClick: () -> Unit = {},
    onDisconnectClick: () -> Unit = {}
) {
    val connectionState = connectionInfo.state
    val isConnected = connectionState == VpnConnectionState.CONNECTED
    val isConnecting = connectionState == VpnConnectionState.CONNECTING
    val isDisconnecting = connectionState == VpnConnectionState.DISCONNECTING
    val isTransitioning = isConnecting || isDisconnecting
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "connection_animation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Color scheme
    val primaryGreen = Color(0xFF00C853)
    val primaryRed = Color(0xFFD32F2F)
    val primaryBlue = Color(0xFF1976D2)
    val warningOrange = Color(0xFFFF6F00)
    
    val statusColor = when (connectionState) {
        VpnConnectionState.CONNECTED -> primaryGreen
        VpnConnectionState.ERROR -> primaryRed
        VpnConnectionState.CONNECTING, VpnConnectionState.DISCONNECTING -> warningOrange
        else -> MaterialTheme.colorScheme.outline
    }
    
    val connectionStatus = when (connectionState) {
        VpnConnectionState.DISCONNECTED -> "Disconnected"
        VpnConnectionState.CONNECTING -> "Connecting..."
        VpnConnectionState.CONNECTED -> "Protected"
        VpnConnectionState.DISCONNECTING -> "Disconnecting..."
        VpnConnectionState.ERROR -> "Connection Failed"
        VpnConnectionState.PERMISSION_REQUIRED -> "Permission Required"
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MarFaNet",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Xray-Powered VPN",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(
                onClick = { /* Settings */ }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Main connection circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.2f),
                            statusColor.copy(alpha = 0.05f)
                        )
                    )
                )
                .clickable(enabled = !isTransitioning) {
                    if (isConnected) {
                        onDisconnectClick()
                    } else {
                        onSmartConnectClick()
                    }
                }
                .scale(if (isTransitioning) pulseScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) {
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryGreen.copy(alpha = 0.3f),
                                    primaryGreen.copy(alpha = 0.1f)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner circle with icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) primaryGreen else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTransitioning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = rotationAngle },
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = when (connectionState) {
                                VpnConnectionState.CONNECTED -> Icons.Default.Shield
                                VpnConnectionState.ERROR -> Icons.Default.ErrorOutline
                                else -> Icons.Default.VpnLock
                            },
                            contentDescription = null,
                            tint = if (isConnected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status text
        Text(
            text = connectionStatus,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = statusColor
        )
        
        // Connection details
        if (isConnected && connectionInfo.profileName != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = connectionInfo.profileName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (connectionInfo.latency != null) {
                        Text(
                            text = "${connectionInfo.latency}ms",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = connectionInfo.protocol?.uppercase() ?: "XRAY",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Smart Connect Button
            OutlinedButton(
                onClick = onSmartConnectClick,
                enabled = !isTransitioning && !isConnected,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart")
            }
            
            // Manual Connect/Disconnect Button
            Button(
                onClick = {
                    if (isConnected) {
                        onDisconnectClick()
                    } else {
                        onConnectClick()
                    }
                },
                enabled = !isTransitioning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) primaryRed else primaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats cards (if connected)
        if (isConnected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    title = "Download",
                    value = formatBytes(connectionInfo.bytesReceived),
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    title = "Upload",
                    value = formatBytes(connectionInfo.bytesSent),
                    icon = Icons.Default.Upload,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Quick actions
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                QuickActionCard(
                    title = "Profiles",
                    icon = Icons.Outlined.List,
                    onClick = { /* Navigate to profiles */ }
                )
            }
            item {
                QuickActionCard(
                    title = "Split Tunnel",
                    icon = Icons.Outlined.Apps,
                    onClick = { /* Navigate to split tunnel */ }
                )
            }
            item {
                QuickActionCard(
                    title = "Statistics",
                    icon = Icons.Outlined.Analytics,
                    onClick = { /* Navigate to stats */ }
                )
            }
            item {
                QuickActionCard(
                    title = "Rules",
                    icon = Icons.Outlined.Rule,
                    onClick = { /* Navigate to routing rules */ }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Text(
            text = "v1.1.0-alpha1 • Built for Iran 🇮🇷",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
        else -> "${bytes / (1024 * 1024 * 1024)}GB"
    }
}


@Composable
fun MarFaNetTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MarFaNetTheme {
        Text("MarFaNet v1.1.0-alpha1")
    }
}
package net.marfanet.android.xray

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xray Core Integration
 * Manages Xray proxy core lifecycle and configuration
 */
@Singleton
class XrayCore @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "XrayCore"
        private const val XRAY_VERSION = "1.8.8"
        private var isLibraryLoaded = false
        
        init {
            try {
                System.loadLibrary("xray")
                isLibraryLoaded = true
                Log.i(TAG, "Xray native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Xray native library not found, using stub implementation: ${e.message}")
            }
        }
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private var isRunning = false
    private var currentConfig: XrayConfig? = null
    
    /**
     * Start Xray core with ProfileEntity
     */
    suspend fun start(profile: net.marfanet.android.data.ProfileEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isRunning) {
                Log.w(TAG, "Xray is already running")
                return@withContext false
            }
            
            // Convert ProfileEntity to XrayConfig
            val config = convertProfileToConfig(profile)
            return@withContext start(config)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Xray with profile", e)
            false
        }
    }
    
    /**
     * Start Xray core with configuration
     */
    suspend fun start(config: XrayConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isRunning) {
                Log.w(TAG, "Xray is already running")
                return@withContext false
            }
            
            val configJson = json.encodeToString(XrayConfig.serializer(), config)
            Log.d(TAG, "Starting Xray with config: $configJson")
            
            val success = if (isLibraryLoaded) {
                nativeStart(configJson)
            } else {
                // Stub implementation for development/testing
                Log.i(TAG, "Using stub Xray implementation")
                true
            }
            
            if (success) {
                isRunning = true
                currentConfig = config
                Log.i(TAG, "Xray core started successfully")
            } else {
                Log.e(TAG, "Failed to start Xray core")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Xray core", e)
            false
        }
    }
    
    /**
     * Stop Xray core
     */
    suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isRunning) {
                Log.w(TAG, "Xray is not running")
                return@withContext true
            }
            
            val success = if (isLibraryLoaded) {
                nativeStop()
            } else {
                // Stub implementation
                true
            }
            
            if (success) {
                isRunning = false
                currentConfig = null
                Log.i(TAG, "Xray core stopped successfully")
            } else {
                Log.e(TAG, "Failed to stop Xray core")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Xray core", e)
            false
        }
    }
    
    /**
     * Check if Xray is running
     */
    fun isRunning(): Boolean = isRunning
    
    /**
     * Get current configuration
     */
    fun getCurrentConfig(): XrayConfig? = currentConfig
    
    /**
     * Get Xray statistics
     */
    suspend fun getStats(): XrayStats = withContext(Dispatchers.IO) {
        if (!isRunning) {
            return@withContext XrayStats()
        }
        
        try {
            val statsJson = if (isLibraryLoaded) {
                nativeGetStats()
            } else {
                // Stub implementation with mock data
                """{"uplink": 1024, "downlink": 2048}"""
            }
            
            json.decodeFromString(XrayStats.serializer(), statsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Xray stats", e)
            XrayStats()
        }
    }
    
    /**
     * Test connectivity to server
     */
    suspend fun testConnectivity(serverAddress: String, serverPort: Int): Long = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            if (isLibraryLoaded) {
                val result = nativeTestConnectivity(serverAddress, serverPort)
                if (result > 0) result else -1
            } else {
                // Stub implementation - simulate ping
                kotlinx.coroutines.delay(50) // Simulate network delay
                System.currentTimeMillis() - startTime
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing connectivity", e)
            -1
        }
    }
    
    /**
     * Process incoming packet through Xray
     */
    fun processPacket(packet: ByteArray, length: Int): ByteArray? {
        return try {
            if (!isRunning) {
                return null
            }
            
            if (isLibraryLoaded) {
                nativeProcessPacket(packet, length)
            } else {
                // Stub implementation - echo packet for testing
                packet.copyOf(length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing packet", e)
            null
        }
    }
    
    /**
     * Get outgoing packets from Xray
     */
    fun getOutgoingPackets(): ByteArray {
        return try {
            if (!isRunning) {
                return ByteArray(0)
            }
            
            if (isLibraryLoaded) {
                nativeGetOutgoingPackets()
            } else {
                // Stub implementation
                ByteArray(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting outgoing packets", e)
            ByteArray(0)
        }
    }
    
    /**
     * Convert ProfileEntity to XrayConfig
     */
    private fun convertProfileToConfig(profile: net.marfanet.android.data.ProfileEntity): XrayConfig {
        val inbounds = listOf(
            InboundConfig(
                tag = "socks-in",
                protocol = "socks",
                listen = "127.0.0.1",
                port = 10808
            ),
            InboundConfig(
                tag = "http-in",
                protocol = "http",
                listen = "127.0.0.1",
                port = 10809
            )
        )
        
        val outbound = when (profile.protocol.lowercase()) {
            "vmess" -> createVmessOutbound(profile)
            "vless" -> createVlessOutbound(profile)
            "trojan" -> createTrojanOutbound(profile)
            "shadowsocks" -> createShadowsocksOutbound(profile)
            else -> createVmessOutbound(profile) // Default to VMess
        }
        
        val outbounds = listOf(
            outbound,
            OutboundConfig(
                tag = "direct",
                protocol = "freedom"
            ),
            OutboundConfig(
                tag = "block",
                protocol = "blackhole"
            )
        )
        
        return XrayConfig(
            inbounds = inbounds,
            outbounds = outbounds,
            routing = createRoutingConfig()
        )
    }
    
    private fun createVmessOutbound(profile: net.marfanet.android.data.ProfileEntity): OutboundConfig {
        val settings = mapOf(
            "vnext" to listOf(
                mapOf(
                    "address" to profile.serverAddress,
                    "port" to profile.serverPort,
                    "users" to listOf(
                        mapOf(
                            "id" to profile.userId,
                            "security" to profile.security,
                            "level" to 0
                        )
                    )
                )
            )
        )
        
        val streamSettings = if (profile.network == "ws") {
            StreamSettings(
                network = "ws",
                security = if (profile.tls == true) "tls" else "none",
                wsSettings = mapOf(
                    "path" to kotlinx.serialization.json.JsonPrimitive(profile.path ?: ""),
                    "headers" to kotlinx.serialization.json.JsonObject(mapOf(
                        "Host" to kotlinx.serialization.json.JsonPrimitive(profile.host ?: "")
                    ))
                ),
                tlsSettings = if (profile.tls == true) mapOf(
                    "serverName" to kotlinx.serialization.json.JsonPrimitive(profile.sni ?: ""),
                    "allowInsecure" to kotlinx.serialization.json.JsonPrimitive(profile.allowInsecure == true)
                ) else null
            )
        } else {
            StreamSettings(
                network = profile.network ?: "tcp",
                security = if (profile.tls == true) "tls" else "none"
            )
        }
        
        return OutboundConfig(
            tag = "proxy",
            protocol = "vmess",
            settings = settings.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value.toString()) },
            streamSettings = streamSettings
        )
    }
    
    private fun createVlessOutbound(profile: net.marfanet.android.data.ProfileEntity): OutboundConfig {
        // Similar to VMess but for VLESS protocol
        return createVmessOutbound(profile).copy(protocol = "vless")
    }
    
    private fun createTrojanOutbound(profile: net.marfanet.android.data.ProfileEntity): OutboundConfig {
        val settings = mapOf(
            "servers" to listOf(
                mapOf(
                    "address" to profile.serverAddress,
                    "port" to profile.serverPort,
                    "password" to profile.password,
                    "level" to 0
                )
            )
        )
        
        return OutboundConfig(
            tag = "proxy",
            protocol = "trojan",
            settings = settings.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value.toString()) }
        )
    }
    
    private fun createShadowsocksOutbound(profile: net.marfanet.android.data.ProfileEntity): OutboundConfig {
        val settings = mapOf(
            "servers" to listOf(
                mapOf(
                    "address" to profile.serverAddress,
                    "port" to profile.serverPort,
                    "method" to profile.security,
                    "password" to profile.password,
                    "level" to 0
                )
            )
        )
        
        return OutboundConfig(
            tag = "proxy",
            protocol = "shadowsocks",
            settings = settings.mapValues { kotlinx.serialization.json.JsonPrimitive(it.value.toString()) }
        )
    }
    
    private fun createRoutingConfig(): RoutingConfig {
        val rules = listOf(
            // Block ads and malware
            RoutingRule(
                domain = listOf("geosite:category-ads-all"),
                outboundTag = "block"
            ),
            // Direct connection for private IPs
            RoutingRule(
                ip = listOf("geoip:private"),
                outboundTag = "direct"
            ),
            // Direct connection for CN domains (if needed)
            RoutingRule(
                domain = listOf("geosite:cn"),
                outboundTag = "direct"
            ),
            // Everything else goes through proxy
            RoutingRule(
                network = "tcp,udp",
                outboundTag = "proxy"
            )
        )
        
        return RoutingConfig(
            domainStrategy = "IPIfNonMatch",
            rules = rules
        )
    }
    
    // Native method declarations
    private external fun nativeStart(configJson: String): Boolean
    private external fun nativeStop(): Boolean
    private external fun nativeGetStats(): String
    private external fun nativeTestConnectivity(address: String, port: Int): Long
    private external fun nativeProcessPacket(packet: ByteArray, length: Int): ByteArray?
    private external fun nativeGetOutgoingPackets(): ByteArray
}

@Serializable
data class XrayConfig(
    val log: LogConfig = LogConfig(),
    val inbounds: List<InboundConfig>,
    val outbounds: List<OutboundConfig>,
    val routing: RoutingConfig = RoutingConfig(),
    val dns: DnsConfig = DnsConfig()
)

@Serializable
data class LogConfig(
    val access: String = "",
    val error: String = "",
    val loglevel: String = "warning"
)

@Serializable
data class InboundConfig(
    val tag: String,
    val protocol: String,
    val listen: String = "127.0.0.1",
    val port: Int = 0,
    val settings: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class OutboundConfig(
    val tag: String,
    val protocol: String,
    val settings: Map<String, JsonElement> = emptyMap(),
    val streamSettings: StreamSettings? = null
)

@Serializable
data class StreamSettings(
    val network: String = "tcp",
    val security: String = "none",
    val tlsSettings: Map<String, JsonElement>? = null,
    val wsSettings: Map<String, JsonElement>? = null
)

@Serializable
data class RoutingConfig(
    val domainStrategy: String = "IPIfNonMatch",
    val rules: List<RoutingRule> = emptyList()
)

@Serializable
data class RoutingRule(
    val type: String = "field",
    val domain: List<String>? = null,
    val ip: List<String>? = null,
    val port: String? = null,
    val network: String? = null,
    val source: List<String>? = null,
    val user: List<String>? = null,
    val inboundTag: List<String>? = null,
    val protocol: List<String>? = null,
    val attrs: String? = null,
    val outboundTag: String,
    val balancerTag: String? = null
)

@Serializable
data class DnsConfig(
    val servers: List<String> = listOf("8.8.8.8", "1.1.1.1"),
    val hosts: Map<String, String> = emptyMap(),
    val clientIp: String? = null,
    val tag: String = "dns_inbound"
)

@Serializable
data class XrayStats(
    val uplink: Long = 0,
    val downlink: Long = 0,
    val uplinkTotal: Long = 0,
    val downlinkTotal: Long = 0
)

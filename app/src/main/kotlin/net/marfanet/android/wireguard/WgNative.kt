package net.marfanet.android.wireguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WireGuard Native JNI Bridge
 * WG-002: JNI interface for wireguard-go integration
 */
class WgNative {
    
    companion object {
        init {
            System.loadLibrary("wg")
        }
        
        // Native method declarations
        @JvmStatic
        external fun wgTurnOn(configStr: String): Int
        
        @JvmStatic
        external fun wgTurnOff(handle: Int): Unit
        
        @JvmStatic
        external fun wgGetConfig(handle: Int): String
        
        @JvmStatic
        external fun wgVersion(): String
        
        @JvmStatic
        external fun wgSetLogger(logLevel: Int): Unit
    }
    
    private var tunnelHandle: Int = -1
    
    /**
     * Start WireGuard tunnel with configuration
     */
    suspend fun startTunnel(config: WgConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val configString = config.toConfigString()
            tunnelHandle = wgTurnOn(configString)
            
            if (tunnelHandle < 0) {
                Result.failure(Exception("Failed to start WireGuard tunnel: $tunnelHandle"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Stop WireGuard tunnel
     */
    suspend fun stopTunnel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (tunnelHandle >= 0) {
                wgTurnOff(tunnelHandle)
                tunnelHandle = -1
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current tunnel configuration
     */
    suspend fun getConfig(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (tunnelHandle < 0) {
                Result.failure(Exception("No active tunnel"))
            } else {
                val config = wgGetConfig(tunnelHandle)
                Result.success(config)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get WireGuard version info
     */
    fun getVersion(): String = wgVersion()
    
    /**
     * Check if tunnel is active
     */
    fun isActive(): Boolean = tunnelHandle >= 0
}

/**
 * WireGuard Configuration Data Class
 */
data class WgConfig(
    val privateKey: String,
    val address: String,
    val dns: List<String> = emptyList(),
    val peers: List<WgPeer> = emptyList()
) {
    fun toConfigString(): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = $privateKey")
        appendLine("Address = $address")
        if (dns.isNotEmpty()) {
            appendLine("DNS = ${dns.joinToString(", ")}")
        }
        
        peers.forEach { peer ->
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${peer.publicKey}")
            appendLine("Endpoint = ${peer.endpoint}")
            if (peer.allowedIPs.isNotEmpty()) {
                appendLine("AllowedIPs = ${peer.allowedIPs.joinToString(", ")}")
            }
            peer.persistentKeepalive?.let {
                appendLine("PersistentKeepalive = $it")
            }
        }
    }
}

data class WgPeer(
    val publicKey: String,
    val endpoint: String,
    val allowedIPs: List<String> = listOf("0.0.0.0/0"),
    val persistentKeepalive: Int? = null
)
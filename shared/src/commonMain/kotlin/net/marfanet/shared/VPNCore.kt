package net.marfanet.shared

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * MarFaNet Shared VPN Core - Kotlin Multiplatform
 * Contains business logic shared across Android, iOS, and Desktop
 */
class SharedVPNCore {
    
    private val json = Json { ignoreUnknownKeys = true }
    private var connectionState = ConnectionState.DISCONNECTED
    
    companion object {
        const val XRAY_VERSION = "1.8.8"
        const val CORE_VERSION = "1.0.0"
    }
    
    /**
     * Parse and validate VPN configuration
     */
    suspend fun parseConfig(configJson: String): VPNConfig {
        return withContext(Dispatchers.Default) {
            try {
                val config = json.decodeFromString<VPNConfig>(configJson)
                validateConfig(config)
                config
            } catch (e: Exception) {
                throw ConfigurationException("Invalid configuration: ${e.message}")
            }
        }
    }
    
    /**
     * Generate Xray configuration for platform-specific implementation
     */
    suspend fun generateXrayConfig(vpnConfig: VPNConfig): String {
        return withContext(Dispatchers.Default) {
            buildString {
                appendLine("{")
                appendLine("  \"log\": {")
                appendLine("    \"access\": \"\",")
                appendLine("    \"error\": \"\",")
                appendLine("    \"loglevel\": \"warning\"")
                appendLine("  },")
                appendLine("  \"inbounds\": [{")
                appendLine("    \"tag\": \"tun\",")
                appendLine("    \"protocol\": \"tun\",")
                appendLine("    \"settings\": {")
                appendLine("      \"address\": \"${vpnConfig.localAddress}\",")
                appendLine("      \"mtu\": ${vpnConfig.mtu}")
                appendLine("    }")
                appendLine("  }],")
                appendLine("  \"outbounds\": [{")
                appendLine("    \"tag\": \"proxy\",")
                appendLine("    \"protocol\": \"${vpnConfig.protocol}\",")
                appendLine("    \"settings\": ${generateProtocolSettings(vpnConfig)}")
                appendLine("  }],")
                appendLine("  \"routing\": {")
                appendLine("    \"rules\": ${generateRoutingRules(vpnConfig)}")
                appendLine("  }")
                appendLine("}")
            }
        }
    }
    
    /**
     * Validate connection parameters
     */
    fun validateConfig(config: VPNConfig): Boolean {
        require(config.serverAddress.isNotBlank()) { "Server address required" }
        require(config.serverPort in 1..65535) { "Invalid port range" }
        require(config.protocol in listOf("vmess", "vless", "trojan", "shadowsocks")) { "Unsupported protocol" }
        require(config.userId.isNotBlank()) { "User ID required" }
        return true
    }
    
    /**
     * Track connection state across platforms
     */
    fun updateConnectionState(state: ConnectionState) {
        connectionState = state
    }
    
    fun getConnectionState(): ConnectionState = connectionState
    
    /**
     * Generate platform-agnostic connection statistics
     */
    suspend fun generateConnectionStats(): ConnectionStats {
        return ConnectionStats(
            state = connectionState,
            uptime = if (connectionState == ConnectionState.CONNECTED) System.currentTimeMillis() else 0,
            bytesReceived = 0, // Would be provided by platform implementation
            bytesSent = 0,
            latency = 0
        )
    }
    
    private fun generateProtocolSettings(config: VPNConfig): String {
        return when (config.protocol) {
            "vmess" -> generateVMessSettings(config)
            "vless" -> generateVLessSettings(config)
            "trojan" -> generateTrojanSettings(config)
            "shadowsocks" -> generateShadowsocksSettings(config)
            else -> throw UnsupportedOperationException("Protocol ${config.protocol} not supported")
        }
    }
    
    private fun generateVMessSettings(config: VPNConfig): String = """
        {
          "vnext": [{
            "address": "${config.serverAddress}",
            "port": ${config.serverPort},
            "users": [{
              "id": "${config.userId}",
              "security": "${config.security}",
              "level": 0
            }]
          }]
        }
    """.trimIndent()
    
    private fun generateVLessSettings(config: VPNConfig): String = """
        {
          "vnext": [{
            "address": "${config.serverAddress}",
            "port": ${config.serverPort},
            "users": [{
              "id": "${config.userId}",
              "encryption": "none",
              "level": 0
            }]
          }]
        }
    """.trimIndent()
    
    private fun generateTrojanSettings(config: VPNConfig): String = """
        {
          "servers": [{
            "address": "${config.serverAddress}",
            "port": ${config.serverPort},
            "password": "${config.password}",
            "level": 0
          }]
        }
    """.trimIndent()
    
    private fun generateShadowsocksSettings(config: VPNConfig): String = """
        {
          "servers": [{
            "address": "${config.serverAddress}",
            "port": ${config.serverPort},
            "method": "${config.security}",
            "password": "${config.password}",
            "level": 0
          }]
        }
    """.trimIndent()
    
    private fun generateRoutingRules(config: VPNConfig): String = """
        [{
          "type": "field",
          "ip": ["geoip:private"],
          "outboundTag": "direct"
        }, {
          "type": "field",
          "ip": ["geoip:cn"],
          "outboundTag": "${if (config.bypassChina) "direct" else "proxy"}"
        }, {
          "type": "field",
          "network": "tcp,udp",
          "outboundTag": "proxy"
        }]
    """.trimIndent()
}

@Serializable
data class VPNConfig(
    val id: String,
    val name: String,
    val protocol: String,
    val serverAddress: String,
    val serverPort: Int,
    val userId: String,
    val password: String = "",
    val security: String = "auto",
    val localAddress: String = "10.0.0.1",
    val mtu: Int = 1500,
    val bypassChina: Boolean = true,
    val gfwKnockEnabled: Boolean = true
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

@Serializable
data class ConnectionStats(
    val state: ConnectionState,
    val uptime: Long,
    val bytesReceived: Long,
    val bytesSent: Long,
    val latency: Long
)

class ConfigurationException(message: String) : Exception(message)
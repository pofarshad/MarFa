package net.marfanet.android.xray

import android.util.Log
import net.marfanet.android.data.ProfileEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xray Configuration Builder
 * Builds Xray configurations from profiles with advanced routing rules
 */
@Singleton
class XrayConfigBuilder @Inject constructor() {
    
    companion object {
        private const val TAG = "XrayConfigBuilder"
        private const val TUN_INTERFACE_NAME = "tun0"
        private const val TUN_ADDRESS = "10.0.0.1"
        private const val TUN_GATEWAY = "10.0.0.2"
        private const val TUN_NETMASK = "255.255.255.0"
        private const val TUN_MTU = 1500
    }
    
    /**
     * Build complete Xray configuration from profile
     */
    fun buildConfig(
        profile: ProfileEntity,
        enableGfwRules: Boolean = true,
        bypassApps: List<String> = emptyList(),
        customRules: List<CustomRoutingRule> = emptyList()
    ): XrayConfig {
        Log.d(TAG, "Building Xray config for profile: ${profile.name}")
        
        return XrayConfig(
            log = buildLogConfig(),
            inbounds = buildInbounds(),
            outbounds = buildOutbounds(profile),
            routing = buildRoutingConfig(enableGfwRules, bypassApps, customRules),
            dns = buildDnsConfig()
        )
    }
    
    private fun buildLogConfig(): LogConfig {
        return LogConfig(
            access = "",
            error = "",
            loglevel = "warning"
        )
    }
    
    private fun buildInbounds(): List<InboundConfig> {
        return listOf(
            // TUN inbound for capturing system traffic
            InboundConfig(
                tag = "tun-in",
                protocol = "tun"
            ),
            // SOCKS inbound for local proxy
            InboundConfig(
                tag = "socks-in",
                protocol = "socks",
                listen = "127.0.0.1",
                port = 10808
            ),
            // HTTP inbound for local proxy
            InboundConfig(
                tag = "http-in",
                protocol = "http",
                listen = "127.0.0.1",
                port = 10809
            )
        )
    }
    
    private fun buildOutbounds(profile: ProfileEntity): List<OutboundConfig> {
        val outbounds = mutableListOf<OutboundConfig>()
        
        // Main proxy outbound
        outbounds.add(buildProxyOutbound(profile))
        
        // Direct outbound for bypassed traffic
        outbounds.add(
            OutboundConfig(
                tag = "direct",
                protocol = "freedom"
            )
        )
        
        // Block outbound for blocked traffic
        outbounds.add(
            OutboundConfig(
                tag = "block",
                protocol = "blackhole"
            )
        )
        
        return outbounds
    }
    
    private fun buildProxyOutbound(profile: ProfileEntity): OutboundConfig {
        return OutboundConfig(
            tag = "proxy",
            protocol = profile.protocol,
            streamSettings = buildStreamSettings(profile)
        )
    }
    
    private fun buildStreamSettings(profile: ProfileEntity): StreamSettings? {
        val network = profile.network ?: "tcp"
        
        return StreamSettings(
            network = network,
            security = if (profile.tls == true) "tls" else "none"
        )
    }
    
    private fun buildRoutingConfig(
        enableGfwRules: Boolean,
        bypassApps: List<String>,
        customRules: List<CustomRoutingRule>
    ): RoutingConfig {
        val rules = mutableListOf<RoutingRule>()
        
        // Private IP ranges - always direct
        rules.add(
            RoutingRule(
                type = "field",
                ip = listOf(
                    "geoip:private",
                    "10.0.0.0/8",
                    "172.16.0.0/12",
                    "192.168.0.0/16",
                    "127.0.0.0/8"
                ),
                outboundTag = "direct"
            )
        )
        
        // Custom rules (highest priority)
        customRules.forEach { customRule ->
            rules.add(
                RoutingRule(
                    type = "field",
                    domain = if (customRule.domains.isNotEmpty()) customRule.domains else null,
                    ip = if (customRule.ips.isNotEmpty()) customRule.ips else null,
                    outboundTag = when (customRule.action) {
                        RoutingAction.DIRECT -> "direct"
                        RoutingAction.PROXY -> "proxy"
                        RoutingAction.BLOCK -> "block"
                    }
                )
            )
        }
        
        // GFW rules for China bypass
        if (enableGfwRules) {
            // China domains - direct
            rules.add(
                RoutingRule(
                    type = "field",
                    domain = listOf(
                        "geosite:cn",
                        "geosite:category-ads-all"
                    ),
                    outboundTag = "direct"
                )
            )
            
            // China IPs - direct
            rules.add(
                RoutingRule(
                    type = "field",
                    ip = listOf("geoip:cn"),
                    outboundTag = "direct"
                )
            )
        }
        
        // Default rule - everything else goes through proxy
        rules.add(
            RoutingRule(
                type = "field",
                network = "tcp,udp",
                outboundTag = "proxy"
            )
        )
        
        return RoutingConfig(
            domainStrategy = "IPIfNonMatch",
            rules = rules
        )
    }
    
    private fun buildDnsConfig(): DnsConfig {
        return DnsConfig(
            servers = listOf(
                "8.8.8.8",
                "1.1.1.1",
                "223.5.5.5", // Alibaba DNS for China
                "119.29.29.29" // Tencent DNS for China
            ),
            hosts = mapOf(
                "domain:googleapis.cn" to "googleapis.com"
            ),
            tag = "dns_inbound"
        )
    }
}

data class CustomRoutingRule(
    val domains: List<String> = emptyList(),
    val ips: List<String> = emptyList(),
    val action: RoutingAction
)

enum class RoutingAction {
    DIRECT,
    PROXY,
    BLOCK
}

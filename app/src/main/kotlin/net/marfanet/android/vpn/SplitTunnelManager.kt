package net.marfanet.android.vpn

import net.marfanet.android.data.AppRule
import net.marfanet.android.data.AppRuleDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Split-Tunneling Manager for VPN Service Integration
 * STU-004: Inject per-app rules into XrayConfigBuilder
 */
@Singleton
class SplitTunnelManager @Inject constructor(
    private val appRuleDao: AppRuleDao
) {
    
    /**
     * Get enabled app rules for VPN routing
     */
    suspend fun getEnabledRules(): List<AppRule> {
        return appRuleDao.getEnabledRules()
    }
    
    /**
     * Generate Xray routing rules for split-tunneling
     */
    suspend fun generateXrayRoutingRules(): XrayRoutingConfig {
        val enabledRules = getEnabledRules()
        
        val bypassApps = enabledRules
            .filter { it.ruleType == AppRule.RuleType.BYPASS }
            .map { it.packageName }
            
        val tunnelApps = enabledRules
            .filter { it.ruleType == AppRule.RuleType.TUNNEL }
            .map { it.packageName }
            
        val blockedApps = enabledRules
            .filter { it.ruleType == AppRule.RuleType.BLOCK }
            .map { it.packageName }
        
        return XrayRoutingConfig(
            bypassApps = bypassApps,
            tunnelApps = tunnelApps,
            blockedApps = blockedApps
        )
    }
}

/**
 * Xray routing configuration for split-tunneling
 */
data class XrayRoutingConfig(
    val bypassApps: List<String>,
    val tunnelApps: List<String>,
    val blockedApps: List<String>
)
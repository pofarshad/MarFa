package net.marfanet.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for split-tunneling app rules
 * STU-002: Room schema implementation for per-app VPN routing
 */
@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val ruleType: RuleType = RuleType.BYPASS,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    enum class RuleType {
        BYPASS,    // Traffic bypasses VPN
        TUNNEL,    // Traffic goes through VPN
        BLOCK      // Traffic is blocked entirely
    }
}

/**
 * Data Access Object for AppRule entities
 * Provides CRUD operations for split-tunneling configuration
 */
@androidx.room.Dao
interface AppRuleDao {
    @androidx.room.Query("SELECT * FROM app_rules ORDER BY appName ASC")
    suspend fun getAllRules(): List<AppRule>
    
    @androidx.room.Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<AppRule>
    
    @androidx.room.Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRuleByPackage(packageName: String): AppRule?
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRule)
    
    @androidx.room.Update
    suspend fun updateRule(rule: AppRule)
    
    @androidx.room.Delete
    suspend fun deleteRule(rule: AppRule)
    
    @androidx.room.Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRuleByPackage(packageName: String)
    
    @androidx.room.Query("UPDATE app_rules SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updateRuleStatus(packageName: String, enabled: Boolean)
}
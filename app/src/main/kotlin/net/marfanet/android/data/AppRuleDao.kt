package net.marfanet.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app routing rules
 */
@Dao
interface AppRuleDao {
    
    @Query("SELECT * FROM app_rules ORDER BY appName ASC")
    fun getAllRules(): Flow<List<AppRule>>
    
    @Query("SELECT * FROM app_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<AppRule>
    
    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getRuleForPackage(packageName: String): AppRule?
    
    @Query("SELECT * FROM app_rules WHERE action = :action")
    suspend fun getRulesByAction(action: String): List<AppRule>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRule)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AppRule>)
    
    @Update
    suspend fun updateRule(rule: AppRule)
    
    @Delete
    suspend fun deleteRule(rule: AppRule)
    
    @Query("DELETE FROM app_rules WHERE id = :id")
    suspend fun deleteRuleById(id: String)
    
    @Query("UPDATE app_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRuleEnabled(id: String, isEnabled: Boolean)
    
    @Query("UPDATE app_rules SET action = :action WHERE packageName = :packageName")
    suspend fun updateRuleAction(packageName: String, action: String)
}

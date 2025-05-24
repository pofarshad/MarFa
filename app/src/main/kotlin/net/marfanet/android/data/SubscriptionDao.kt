package net.marfanet.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for subscription groups
 */
@Dao
interface SubscriptionDao {
    
    @Query("SELECT * FROM subscription_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<SubscriptionGroupEntity>>
    
    @Query("SELECT * FROM subscription_groups WHERE id = :id")
    suspend fun getGroupById(id: String): SubscriptionGroupEntity?
    
    @Query("SELECT * FROM subscription_groups WHERE isActive = 1")
    suspend fun getActiveGroups(): List<SubscriptionGroupEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SubscriptionGroupEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<SubscriptionGroupEntity>)
    
    @Update
    suspend fun updateGroup(group: SubscriptionGroupEntity)
    
    @Delete
    suspend fun deleteGroup(group: SubscriptionGroupEntity)
    
    @Query("DELETE FROM subscription_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)
    
    @Query("UPDATE subscription_groups SET lastUpdated = :timestamp, profileCount = :count WHERE id = :id")
    suspend fun updateGroupStats(id: String, timestamp: Long, count: Int)
    
    @Query("UPDATE subscription_groups SET isActive = :isActive WHERE id = :id")
    suspend fun setGroupActive(id: String, isActive: Boolean)
}

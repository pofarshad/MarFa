package net.marfanet.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VPN profiles
 */
@Dao
interface ProfileDao {
    
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>
    
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAllProfilesList(): List<ProfileEntity>
    
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?
    
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): ProfileEntity?
    
    @Query("SELECT * FROM profiles WHERE subscriptionGroup = :groupId")
    suspend fun getProfilesByGroup(groupId: String): List<ProfileEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)
    
    @Update
    suspend fun updateProfile(profile: ProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)
    
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)
    
    @Query("DELETE FROM profiles WHERE subscriptionGroup = :groupId")
    suspend fun deleteProfilesByGroup(groupId: String)
    
    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()
    
    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: String)
    
    @Query("UPDATE profiles SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: String, latency: Long)
    
    @Query("UPDATE profiles SET lastConnected = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: String, timestamp: Long)
}

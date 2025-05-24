package net.marfanet.android.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for profile statistics
 */
@Dao
interface ProfileStatsDao {
    
    @Query("SELECT * FROM profile_stats WHERE profileId = :profileId")
    suspend fun getStatsForProfile(profileId: String): ProfileStatsEntity?
    
    @Query("SELECT * FROM profile_stats ORDER BY totalConnections DESC")
    fun getAllStats(): Flow<List<ProfileStatsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: ProfileStatsEntity)
    
    @Update
    suspend fun updateStats(stats: ProfileStatsEntity)
    
    @Delete
    suspend fun deleteStats(stats: ProfileStatsEntity)
    
    @Query("DELETE FROM profile_stats WHERE profileId = :profileId")
    suspend fun deleteStatsForProfile(profileId: String)
    
    @Query("UPDATE profile_stats SET totalConnections = totalConnections + 1, lastConnectionTime = :timestamp WHERE profileId = :profileId")
    suspend fun incrementConnectionCount(profileId: String, timestamp: Long)
    
    @Query("UPDATE profile_stats SET successfulConnections = successfulConnections + 1 WHERE profileId = :profileId")
    suspend fun incrementSuccessfulConnections(profileId: String)
    
    @Query("UPDATE profile_stats SET failedConnections = failedConnections + 1 WHERE profileId = :profileId")
    suspend fun incrementFailedConnections(profileId: String)
    
    @Query("UPDATE profile_stats SET totalUpload = totalUpload + :upload, totalDownload = totalDownload + :download WHERE profileId = :profileId")
    suspend fun updateDataUsage(profileId: String, upload: Long, download: Long)
    
    @Query("UPDATE profile_stats SET averageLatency = :latency WHERE profileId = :profileId")
    suspend fun updateAverageLatency(profileId: String, latency: Long)
}

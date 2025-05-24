package net.marfanet.android.service

import android.content.Context
import android.util.Log
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.marfanet.android.data.ProfileDao
import net.marfanet.android.di.IoDispatcher
import net.marfanet.android.xray.XrayCore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.content.Context as AndroidContext
import android.util.Log as AndroidLog

/**
 * Background service for monitoring server latency and updating profiles
 */
@Singleton
class PingService @Inject constructor(
    @ApplicationContext private val context: AndroidContext,
    private val profileDao: ProfileDao,
    private val xrayCore: XrayCore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "PingService"
        private const val PING_WORK_NAME = "ping_servers"
        private const val PING_INTERVAL_MINUTES = 10L
        private const val PING_TIMEOUT_MS = 5000L
    }
    
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Start periodic ping monitoring
     */
    fun startPingMonitoring() {
        AndroidLog.d(TAG, "Starting ping monitoring service")
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val pingRequest = PeriodicWorkRequestBuilder<PingWorker>(
            PING_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            PING_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            pingRequest
        )
    }
    
    /**
     * Stop ping monitoring
     */
    fun stopPingMonitoring() {
        AndroidLog.d(TAG, "Stopping ping monitoring service")
        workManager.cancelUniqueWork(PING_WORK_NAME)
    }
    
    /**
     * Perform immediate ping test on all profiles
     */
    suspend fun pingAllProfiles(): Map<String, Long> = withContext(dispatcher) {
        val results = mutableMapOf<String, Long>()
        
        try {
            val profiles = profileDao.getAllProfilesList()
            AndroidLog.d(TAG, "Pinging ${profiles.size} profiles")
            
            profiles.forEach { profile ->
                try {
                    val latency = xrayCore.testConnectivity(
                        profile.serverAddress,
                        profile.serverPort
                    )
                    
                    results[profile.id] = latency
                    
                    // Update profile with new latency
                    if (latency > 0) {
                        profileDao.updateLatency(profile.id, latency)
                        AndroidLog.d(TAG, "Updated ${profile.name} latency: ${latency}ms")
                    } else {
                        AndroidLog.w(TAG, "Failed to ping ${profile.name}")
                    }
                    
                } catch (e: Exception) {
                    AndroidLog.e(TAG, "Error pinging ${profile.name}", e)
                    results[profile.id] = -1L
                }
            }
            
        } catch (e: Exception) {
            AndroidLog.e(TAG, "Error in ping all profiles", e)
        }
        
        results
    }
    
    /**
     * Get the best available server based on latency
     */
    suspend fun getBestServer(): String? = withContext(dispatcher) {
        try {
            val profiles = profileDao.getAllProfilesList()
            val bestProfile = profiles
                .filter { it.latency != null && it.latency!! > 0 }
                .minByOrNull { it.latency!! }
            
            bestProfile?.let {
                Log.d(TAG, "Best server: ${it.name} (${it.latency}ms)")
                it.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting best server", e)
            null
        }
    }
}

/**
 * Background worker for periodic ping tests
 */
class PingWorker @AssistedInject constructor(
    @Assisted context: AndroidContext,
    @Assisted workerParams: WorkerParameters,
    private val pingService: PingService
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "PingWorker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            AndroidLog.d(TAG, "Starting periodic ping work")
            
            val results = pingService.pingAllProfiles()
            val successCount = results.values.count { it > 0 }
            val totalCount = results.size
            
            AndroidLog.d(TAG, "Ping work completed: $successCount/$totalCount servers responded")
            
            Result.success()
        } catch (e: Exception) {
            AndroidLog.e(TAG, "Ping work failed", e)
            Result.retry()
        }
    }
    
    @AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): PingWorker
    }
}

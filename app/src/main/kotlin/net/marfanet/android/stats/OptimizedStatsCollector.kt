package net.marfanet.android.stats

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized VPN Statistics Collector
 * Implements adaptive polling and battery-aware monitoring
 */
@Singleton
class OptimizedStatsCollector @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isCollecting = AtomicBoolean(false)
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    private val _statsFlow = MutableStateFlow(VpnStatsModel())
    val statsFlow: StateFlow<VpnStatsModel> = _statsFlow.asStateFlow()
    
    // Adaptive polling intervals based on battery and performance
    private var currentPollInterval = DEFAULT_POLL_INTERVAL
    private var lastUploadSpeed = 0L
    private var lastDownloadSpeed = 0L
    
    companion object {
        private const val DEFAULT_POLL_INTERVAL = 1000L // 1 second
        private const val MAX_POLL_INTERVAL = 5000L // 5 seconds
        private const val MIN_POLL_INTERVAL = 500L // 0.5 seconds
        private const val BATTERY_THRESHOLD = 20 // 20% battery level
    }
    
    /**
     * Start collecting VPN statistics with adaptive polling
     */
    fun startCollection() {
        if (isCollecting.getAndSet(true)) return
        
        scope.launch {
            while (isActive && isCollecting.get()) {
                try {
                    val stats = collectStats()
                    _statsFlow.value = stats
                    
                    // Adjust polling interval based on conditions
                    adaptPollingInterval(stats)
                    
                    delay(currentPollInterval)
                } catch (e: Exception) {
                    // Log error but continue collecting
                    delay(currentPollInterval)
                }
            }
        }
    }
    
    /**
     * Stop collecting statistics
     */
    fun stopCollection() {
        isCollecting.set(false)
    }
    
    /**
     * Collect current VPN statistics with memory optimization
     */
    private suspend fun collectStats(): VpnStatsModel = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        
        // Measure network speeds efficiently
        val (uploadSpeed, downloadSpeed) = measureNetworkSpeeds()
        
        // Measure RTT only when needed (based on interval)
        val rtt = if (currentPollInterval <= 1000L) {
            measureRtt()
        } else {
            _statsFlow.value.rtt
        }
        
        VpnStatsModel(
            rtt = rtt,
            uploadSpeed = uploadSpeed,
            downloadSpeed = downloadSpeed,
            timestamp = currentTime,
            batteryLevel = getBatteryLevel(),
            isLowPowerMode = isLowPowerMode()
        )
    }
    
    /**
     * Adapt polling interval based on conditions
     */
    private fun adaptPollingInterval(stats: VpnStatsModel) {
        // Adjust based on battery level
        if (stats.batteryLevel <= BATTERY_THRESHOLD || stats.isLowPowerMode) {
            currentPollInterval = maxOf(currentPollInterval, 2000L)
            return
        }
        
        // Adjust based on network activity
        val speedDelta = kotlin.math.abs(stats.uploadSpeed - lastUploadSpeed) +
                        kotlin.math.abs(stats.downloadSpeed - lastDownloadSpeed)
        
        currentPollInterval = when {
            speedDelta > 1024 * 1024 -> // High activity
                (currentPollInterval * 0.8).toLong().coerceIn(MIN_POLL_INTERVAL, MAX_POLL_INTERVAL)
            speedDelta < 1024 * 10 -> // Low activity
                (currentPollInterval * 1.2).toLong().coerceIn(MIN_POLL_INTERVAL, MAX_POLL_INTERVAL)
            else -> currentPollInterval
        }
        
        lastUploadSpeed = stats.uploadSpeed
        lastDownloadSpeed = stats.downloadSpeed
    }
    
    /**
     * Measure network speeds efficiently using TrafficStats
     */
    private fun measureNetworkSpeeds(): Pair<Long, Long> {
        return try {
            val upload = android.net.TrafficStats.getTotalTxBytes()
            val download = android.net.TrafficStats.getTotalRxBytes()
            Pair(upload, download)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    /**
     * Measure RTT with timeout
     */
    private suspend fun measureRtt(): Long = withTimeoutOrNull(1000L) {
        try {
            val startTime = System.currentTimeMillis()
            val reachable = withContext(Dispatchers.IO) {
                java.net.InetAddress.getByName("8.8.8.8").isReachable(1000)
            }
            if (reachable) System.currentTimeMillis() - startTime else -1L
        } catch (e: Exception) {
            -1L
        }
    } ?: -1L
    
    /**
     * Get current battery level
     */
    private fun getBatteryLevel(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    /**
     * Check if device is in low power mode
     */
    private fun isLowPowerMode(): Boolean {
        return powerManager.isPowerSaveMode
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopCollection()
        scope.cancel()
    }
}

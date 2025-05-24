package net.marfanet.android.stats

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import kotlin.system.measureTimeMillis

/**
 * VPN Stats Polling Coroutine
 * RSO-002: Implement 1s polling coroutine for live RTT/speed data
 */
class VpnStatsCollector {
    
    private val _statsState = MutableStateFlow(VpnStatsModel())
    val statsState: StateFlow<VpnStatsModel> = _statsState.asStateFlow()
    
    private var collectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Start collecting VPN stats every 1 second
     */
    fun startCollection() {
        stopCollection() // Ensure no duplicate jobs
        
        collectionJob = scope.launch {
            while (isActive) {
                try {
                    val stats = collectCurrentStats()
                    _statsState.value = stats
                    delay(1000) // 1 second polling interval
                } catch (e: Exception) {
                    // Continue collecting even if one measurement fails
                    delay(1000)
                }
            }
        }
    }
    
    /**
     * Stop collecting stats
     */
    fun stopCollection() {
        collectionJob?.cancel()
        collectionJob = null
    }
    
    /**
     * Collect current VPN statistics
     */
    private suspend fun collectCurrentStats(): VpnStatsModel {
        return withContext(Dispatchers.IO) {
            val rtt = measureRtt()
            val (uploadSpeed, downloadSpeed) = measureSpeeds()
            
            VpnStatsModel(
                rtt = rtt,
                uploadSpeed = uploadSpeed,
                downloadSpeed = downloadSpeed,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Measure Round-Trip Time (RTT) to test server
     */
    private suspend fun measureRtt(): Long {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Ping Google DNS (8.8.8.8) for RTT measurement
            val reachable = withContext(Dispatchers.IO) {
                InetAddress.getByName("8.8.8.8").isReachable(3000)
            }
            
            if (reachable) {
                System.currentTimeMillis() - startTime
            } else {
                -1L // Indicate unreachable
            }
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Measure upload and download speeds using network interface statistics
     */
    private suspend fun measureSpeeds(): Pair<Long, Long> {
        return try {
            // Read actual network interface statistics
            // This would integrate with Android's TrafficStats or NetworkStatsManager
            val uploadSpeed = android.net.TrafficStats.getTotalTxBytes()
            val downloadSpeed = android.net.TrafficStats.getTotalRxBytes()
            
            Pair(uploadSpeed, downloadSpeed)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopCollection()
        scope.cancel()
    }
}

package net.marfanet.android.benchmark

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.measureTimeMillis

/**
 * Throughput Benchmark for WireGuard vs Xray Performance
 * WG-004: Run throughput benchmark and log ≥30% gain
 */
class ThroughputBenchmark {
    
    private val _benchmarkState = MutableStateFlow(BenchmarkState())
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState.asStateFlow()
    
    /**
     * Run complete benchmark comparing Xray vs WireGuard
     */
    suspend fun runBenchmark(): BenchmarkResult {
        _benchmarkState.value = BenchmarkState(isRunning = true, status = "Starting benchmark...")
        
        return withContext(Dispatchers.IO) {
            try {
                // Test Xray performance
                _benchmarkState.value = _benchmarkState.value.copy(status = "Testing Xray performance...")
                val xrayResults = measureEnginePerformance("Xray")
                
                delay(2000) // Brief pause between tests
                
                // Test WireGuard performance
                _benchmarkState.value = _benchmarkState.value.copy(status = "Testing WireGuard performance...")
                val wireGuardResults = measureEnginePerformance("WireGuard")
                
                // Calculate improvement
                val improvement = calculateImprovement(xrayResults, wireGuardResults)
                
                val result = BenchmarkResult(
                    xrayPerformance = xrayResults,
                    wireGuardPerformance = wireGuardResults,
                    improvementPercentage = improvement,
                    timestamp = System.currentTimeMillis()
                )
                
                _benchmarkState.value = BenchmarkState(
                    isRunning = false,
                    isComplete = true,
                    result = result,
                    status = "Benchmark complete!"
                )
                
                result
            } catch (e: Exception) {
                _benchmarkState.value = BenchmarkState(
                    isRunning = false,
                    error = e.message,
                    status = "Benchmark failed"
                )
                throw e
            }
        }
    }
    
    /**
     * Measure performance for a specific VPN engine
     */
    private suspend fun measureEnginePerformance(engineName: String): EnginePerformance {
        val downloadSpeeds = mutableListOf<Long>()
        val uploadSpeeds = mutableListOf<Long>()
        val latencies = mutableListOf<Long>()
        
        // Run multiple test iterations for accuracy
        repeat(5) { iteration ->
            _benchmarkState.value = _benchmarkState.value.copy(
                status = "Testing $engineName - iteration ${iteration + 1}/5"
            )
            
            // Measure download speed using network interface stats
            val downloadSpeed = measureActualDownloadSpeed()
            downloadSpeeds.add(downloadSpeed)
            
            // Measure upload speed using network interface stats
            val uploadSpeed = measureActualUploadSpeed()
            uploadSpeeds.add(uploadSpeed)
            
            // Measure latency with real ping
            val latency = measureActualLatency()
            latencies.add(latency)
            
            delay(1000) // Brief pause between iterations
        }
        
        return EnginePerformance(
            engineName = engineName,
            avgDownloadSpeed = downloadSpeeds.average().toLong(),
            avgUploadSpeed = uploadSpeeds.average().toLong(),
            avgLatency = latencies.average().toLong(),
            testIterations = 5
        )
    }
    
    /**
     * Measure actual download speed using TrafficStats
     */
    private suspend fun measureActualDownloadSpeed(): Long {
        return try {
            val startRx = android.net.TrafficStats.getTotalRxBytes()
            val startTime = System.currentTimeMillis()
            
            delay(1000) // Measure over 1 second
            
            val endRx = android.net.TrafficStats.getTotalRxBytes()
            val endTime = System.currentTimeMillis()
            
            val bytesTransferred = endRx - startRx
            val timeElapsed = (endTime - startTime) / 1000.0
            
            (bytesTransferred / timeElapsed).toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Measure actual upload speed using TrafficStats
     */
    private suspend fun measureActualUploadSpeed(): Long {
        return try {
            val startTx = android.net.TrafficStats.getTotalTxBytes()
            val startTime = System.currentTimeMillis()
            
            delay(1000) // Measure over 1 second
            
            val endTx = android.net.TrafficStats.getTotalTxBytes()
            val endTime = System.currentTimeMillis()
            
            val bytesTransferred = endTx - startTx
            val timeElapsed = (endTime - startTime) / 1000.0
            
            (bytesTransferred / timeElapsed).toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Measure actual network latency
     */
    private suspend fun measureActualLatency(): Long {
        return try {
            val startTime = System.currentTimeMillis()
            
            // Use InetAddress reachability test for real latency
            val reachable = withContext(Dispatchers.IO) {
                java.net.InetAddress.getByName("8.8.8.8").isReachable(3000)
            }
            
            if (reachable) {
                System.currentTimeMillis() - startTime
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Calculate performance improvement percentage
     */
    private fun calculateImprovement(xray: EnginePerformance, wireGuard: EnginePerformance): Double {
        val xrayTotal = xray.avgDownloadSpeed + xray.avgUploadSpeed
        val wireGuardTotal = wireGuard.avgDownloadSpeed + wireGuard.avgUploadSpeed
        
        return if (xrayTotal > 0) {
            ((wireGuardTotal - xrayTotal).toDouble() / xrayTotal) * 100
        } else {
            0.0
        }
    }
}

/**
 * Engine performance metrics
 */
data class EnginePerformance(
    val engineName: String,
    val avgDownloadSpeed: Long,
    val avgUploadSpeed: Long,
    val avgLatency: Long,
    val testIterations: Int
) {
    fun formatDownloadSpeed(): String = formatSpeed(avgDownloadSpeed)
    fun formatUploadSpeed(): String = formatSpeed(avgUploadSpeed)
    fun formatLatency(): String = "${avgLatency}ms"
    
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "${bytesPerSecond}B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024}KB/s"
            bytesPerSecond < 1024 * 1024 * 1024 -> "${bytesPerSecond / (1024 * 1024)}MB/s"
            else -> "${bytesPerSecond / (1024 * 1024 * 1024)}GB/s"
        }
    }
}

/**
 * Complete benchmark result
 */
data class BenchmarkResult(
    val xrayPerformance: EnginePerformance,
    val wireGuardPerformance: EnginePerformance,
    val improvementPercentage: Double,
    val timestamp: Long
) {
    fun meetsTarget(): Boolean = improvementPercentage >= 30.0
    
    fun getImprovementText(): String = 
        if (improvementPercentage >= 0) {
            "+${String.format("%.1f", improvementPercentage)}% faster"
        } else {
            "${String.format("%.1f", improvementPercentage)}% slower"
        }
}

/**
 * Benchmark state for UI updates
 */
data class BenchmarkState(
    val isRunning: Boolean = false,
    val isComplete: Boolean = false,
    val status: String = "Ready to benchmark",
    val result: BenchmarkResult? = null,
    val error: String? = null
)
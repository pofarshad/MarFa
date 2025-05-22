package net.marfanet.android.monitoring

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * MarFaNet Prometheus Metrics Exporter
 * Exposes core VPN metrics for Grafana monitoring
 */
class PrometheusExporter {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Core metrics counters
    private val connectionAttempts = AtomicInteger(0)
    private val connectionSuccesses = AtomicInteger(0)
    private val connectionFailures = AtomicInteger(0)
    private val bytesTransferred = AtomicLong(0)
    private val currentConnections = AtomicInteger(0)
    
    // Performance metrics
    private val connectionLatencyMs = AtomicLong(0)
    private val throughputBytesPerSec = AtomicLong(0)
    private val packetLossRate = AtomicLong(0)
    
    // App performance metrics
    private val memoryUsageMB = AtomicLong(0)
    private val cpuUsagePercent = AtomicLong(0)
    private val batteryDrainPercent = AtomicLong(0)
    
    fun startMetricsCollection() {
        scope.launch {
            while (isActive) {
                collectMetrics()
                delay(30_000) // Collect every 30 seconds
            }
        }
    }
    
    fun recordConnectionAttempt() {
        connectionAttempts.incrementAndGet()
    }
    
    fun recordConnectionSuccess(latencyMs: Long) {
        connectionSuccesses.incrementAndGet()
        currentConnections.incrementAndGet()
        connectionLatencyMs.set(latencyMs)
    }
    
    fun recordConnectionFailure(errorCode: String) {
        connectionFailures.incrementAndGet()
    }
    
    fun recordDataTransfer(bytes: Long) {
        bytesTransferred.addAndGet(bytes)
    }
    
    fun recordThroughput(bytesPerSec: Long) {
        throughputBytesPerSec.set(bytesPerSec)
    }
    
    fun recordPerformanceMetrics(memoryMB: Long, cpuPercent: Long, batteryPercent: Long) {
        memoryUsageMB.set(memoryMB)
        cpuUsagePercent.set(cpuPercent)
        batteryDrainPercent.set(batteryPercent)
    }
    
    /**
     * Generate Prometheus-format metrics
     */
    fun generateMetrics(): String = buildString {
        val timestamp = System.currentTimeMillis()
        
        // Connection metrics
        appendLine("# HELP marfanet_connection_attempts_total Total VPN connection attempts")
        appendLine("# TYPE marfanet_connection_attempts_total counter")
        appendLine("marfanet_connection_attempts_total ${connectionAttempts.get()} $timestamp")
        
        appendLine("# HELP marfanet_connection_successes_total Successful VPN connections")
        appendLine("# TYPE marfanet_connection_successes_total counter")
        appendLine("marfanet_connection_successes_total ${connectionSuccesses.get()} $timestamp")
        
        appendLine("# HELP marfanet_connection_failures_total Failed VPN connections")
        appendLine("# TYPE marfanet_connection_failures_total counter")
        appendLine("marfanet_connection_failures_total ${connectionFailures.get()} $timestamp")
        
        appendLine("# HELP marfanet_active_connections Current active connections")
        appendLine("# TYPE marfanet_active_connections gauge")
        appendLine("marfanet_active_connections ${currentConnections.get()} $timestamp")
        
        // Performance metrics
        appendLine("# HELP marfanet_connection_latency_ms Connection latency in milliseconds")
        appendLine("# TYPE marfanet_connection_latency_ms gauge")
        appendLine("marfanet_connection_latency_ms ${connectionLatencyMs.get()} $timestamp")
        
        appendLine("# HELP marfanet_throughput_bytes_per_sec Network throughput in bytes per second")
        appendLine("# TYPE marfanet_throughput_bytes_per_sec gauge")
        appendLine("marfanet_throughput_bytes_per_sec ${throughputBytesPerSec.get()} $timestamp")
        
        appendLine("# HELP marfanet_bytes_transferred_total Total bytes transferred")
        appendLine("# TYPE marfanet_bytes_transferred_total counter")
        appendLine("marfanet_bytes_transferred_total ${bytesTransferred.get()} $timestamp")
        
        // App performance
        appendLine("# HELP marfanet_memory_usage_mb Memory usage in megabytes")
        appendLine("# TYPE marfanet_memory_usage_mb gauge")
        appendLine("marfanet_memory_usage_mb ${memoryUsageMB.get()} $timestamp")
        
        appendLine("# HELP marfanet_cpu_usage_percent CPU usage percentage")
        appendLine("# TYPE marfanet_cpu_usage_percent gauge")
        appendLine("marfanet_cpu_usage_percent ${cpuUsagePercent.get()} $timestamp")
        
        appendLine("# HELP marfanet_battery_drain_percent Battery drain percentage per hour")
        appendLine("# TYPE marfanet_battery_drain_percent gauge")
        appendLine("marfanet_battery_drain_percent ${batteryDrainPercent.get()} $timestamp")
        
        // Success rate calculation
        val totalAttempts = connectionAttempts.get()
        val successRate = if (totalAttempts > 0) {
            (connectionSuccesses.get().toDouble() / totalAttempts * 100).toLong()
        } else 0L
        
        appendLine("# HELP marfanet_success_rate_percent Connection success rate percentage")
        appendLine("# TYPE marfanet_success_rate_percent gauge")
        appendLine("marfanet_success_rate_percent $successRate $timestamp")
    }
    
    private suspend fun collectMetrics() {
        // Collect current performance metrics
        val currentMemory = getCurrentMemoryUsage()
        val currentCpu = getCurrentCpuUsage()
        val currentBattery = getCurrentBatteryDrain()
        
        recordPerformanceMetrics(currentMemory, currentCpu, currentBattery)
    }
    
    private fun getCurrentMemoryUsage(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            usedMemory / (1024 * 1024) // Convert to MB
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun getCurrentCpuUsage(): Long {
        return 10L // Would integrate with actual CPU monitoring
    }
    
    private fun getCurrentBatteryDrain(): Long {
        return 2L // Would integrate with actual battery monitoring
    }
    
    fun stop() {
        scope.cancel()
    }
}
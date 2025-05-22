package net.marfanet.android.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * MarFaNet Connection Watchdog & JSON Structured Logger
 * Provides structured logging for disconnect root-cause analysis
 * Implements P1-03: Connection Watchdog JSON Logs
 */
class ConnectionLogger(private val context: Context) {
    
    private val logQueue = ConcurrentLinkedQueue<ConnectionLogEntry>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val logFile: File by lazy {
        File(context.filesDir, "connection_logs.jsonl")
    }
    
    private var isLogging = false
    
    companion object {
        private const val TAG = "ConnectionLogger"
        private const val MAX_LOG_SIZE_MB = 10
        private const val MAX_LOG_ENTRIES = 1000
        private const val LOG_FLUSH_INTERVAL_MS = 5000L
    }
    
    init {
        startLogProcessor()
    }
    
    /**
     * Log connection attempt with structured data
     */
    fun logConnectionAttempt(
        serverId: String,
        protocol: String,
        serverAddress: String,
        serverPort: Int
    ) {
        val entry = ConnectionLogEntry(
            timestamp = System.currentTimeMillis(),
            eventType = "connection_attempt",
            serverId = serverId,
            protocol = protocol,
            serverAddress = serverAddress,
            serverPort = serverPort,
            connectionStatus = "attempting"
        )
        
        queueLogEntry(entry)
        Log.d(TAG, "Connection attempt: $serverId ($protocol)")
    }
    
    /**
     * Log successful connection with timing details
     */
    fun logConnectionSuccess(
        serverId: String,
        protocol: String,
        connectionTimeMs: Long,
        selectedServer: String
    ) {
        val entry = ConnectionLogEntry(
            timestamp = System.currentTimeMillis(),
            eventType = "connection_success",
            serverId = serverId,
            protocol = protocol,
            connectionStatus = "connected",
            connectionTimeMs = connectionTimeMs,
            selectedServer = selectedServer
        )
        
        queueLogEntry(entry)
        Log.i(TAG, "Connection successful: $serverId in ${connectionTimeMs}ms")
    }
    
    /**
     * Log connection failure with detailed error information
     */
    fun logConnectionFailure(
        serverId: String,
        protocol: String,
        errorCode: String,
        errorMessage: String,
        retryAttempt: Int = 0,
        networkType: String? = null
    ) {
        val entry = ConnectionLogEntry(
            timestamp = System.currentTimeMillis(),
            eventType = "connection_failure",
            serverId = serverId,
            protocol = protocol,
            connectionStatus = "failed",
            disconnectReason = DisconnectReason(
                code = errorCode,
                message = errorMessage,
                category = categorizeError(errorCode),
                isRetryable = isRetryableError(errorCode)
            ),
            retryAttempt = retryAttempt,
            networkType = networkType
        )
        
        queueLogEntry(entry)
        Log.w(TAG, "Connection failed: $serverId - $errorCode: $errorMessage")
    }
    
    /**
     * Log unexpected disconnection with root cause analysis
     */
    fun logDisconnection(
        serverId: String,
        protocol: String,
        sessionDurationMs: Long,
        disconnectReason: String,
        errorCode: String? = null,
        networkChange: Boolean = false,
        bytesTransferred: Long = 0,
        isUserInitiated: Boolean = false
    ) {
        val entry = ConnectionLogEntry(
            timestamp = System.currentTimeMillis(),
            eventType = "disconnection",
            serverId = serverId,
            protocol = protocol,
            connectionStatus = "disconnected",
            sessionDurationMs = sessionDurationMs,
            disconnectReason = DisconnectReason(
                code = errorCode ?: "user_disconnect",
                message = disconnectReason,
                category = if (isUserInitiated) "user_action" else categorizeDisconnect(disconnectReason),
                isRetryable = !isUserInitiated && isRetryableDisconnect(disconnectReason)
            ),
            networkChange = networkChange,
            bytesTransferred = bytesTransferred,
            isUserInitiated = isUserInitiated
        )
        
        queueLogEntry(entry)
        Log.i(TAG, "Disconnection: $serverId after ${sessionDurationMs}ms - $disconnectReason")
    }
    
    private fun queueLogEntry(entry: ConnectionLogEntry) {
        logQueue.offer(entry)
        
        // Limit queue size to prevent memory issues
        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.poll()
        }
    }
    
    private fun startLogProcessor() {
        if (isLogging) return
        isLogging = true
        
        scope.launch {
            while (isActive) {
                try {
                    flushLogQueue()
                    delay(LOG_FLUSH_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "Log processing error", e)
                    delay(LOG_FLUSH_INTERVAL_MS * 2)
                }
            }
        }
    }
    
    private suspend fun flushLogQueue() {
        if (logQueue.isEmpty()) return
        
        withContext(Dispatchers.IO) {
            val entriesToWrite = mutableListOf<ConnectionLogEntry>()
            
            while (logQueue.isNotEmpty() && entriesToWrite.size < 100) {
                logQueue.poll()?.let { entriesToWrite.add(it) }
            }
            
            if (entriesToWrite.isEmpty()) return@withContext
            
            try {
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_MB * 1024 * 1024) {
                    rotateLogFile()
                }
                
                logFile.appendText(
                    entriesToWrite.joinToString("\n") { json.encodeToString(it) } + "\n"
                )
                
                Log.d(TAG, "Flushed ${entriesToWrite.size} log entries")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log entries", e)
                entriesToWrite.forEach { logQueue.offer(it) }
            }
        }
    }
    
    private fun rotateLogFile() {
        try {
            val backupFile = File(context.filesDir, "connection_logs.jsonl.old")
            if (backupFile.exists()) {
                backupFile.delete()
            }
            logFile.renameTo(backupFile)
            Log.d(TAG, "Log file rotated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    private fun categorizeError(errorCode: String): String {
        return when {
            errorCode.contains("timeout", ignoreCase = true) -> "timeout"
            errorCode.contains("network", ignoreCase = true) -> "network"
            errorCode.contains("auth", ignoreCase = true) -> "authentication"
            errorCode.contains("dns", ignoreCase = true) -> "dns"
            errorCode.contains("ssl") || errorCode.contains("tls") -> "encryption"
            else -> "unknown"
        }
    }
    
    private fun categorizeDisconnect(reason: String): String {
        return when {
            reason.contains("network", ignoreCase = true) -> "network_change"
            reason.contains("timeout", ignoreCase = true) -> "timeout"
            reason.contains("server", ignoreCase = true) -> "server_error"
            reason.contains("auth", ignoreCase = true) -> "authentication"
            else -> "unknown"
        }
    }
    
    private fun isRetryableError(errorCode: String): Boolean {
        return when (categorizeError(errorCode)) {
            "timeout", "network", "dns" -> true
            "authentication" -> false
            else -> true
        }
    }
    
    private fun isRetryableDisconnect(reason: String): Boolean {
        return when (categorizeDisconnect(reason)) {
            "network_change", "timeout", "server_error" -> true
            "authentication" -> false
            else -> true
        }
    }
    
    fun cleanup() {
        scope.cancel()
        isLogging = false
    }
}

@Serializable
data class ConnectionLogEntry(
    val timestamp: Long,
    val eventType: String,
    val serverId: String,
    val protocol: String? = null,
    val serverAddress: String? = null,
    val serverPort: Int? = null,
    val connectionStatus: String,
    val connectionTimeMs: Long? = null,
    val sessionDurationMs: Long? = null,
    val disconnectReason: DisconnectReason? = null,
    val retryAttempt: Int? = null,
    val networkType: String? = null,
    val networkChange: Boolean? = null,
    val bytesTransferred: Long? = null,
    val isUserInitiated: Boolean? = null,
    val selectedServer: String? = null
) {
    val timestampIso: String
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(timestamp))
}

@Serializable
data class DisconnectReason(
    val code: String,
    val message: String,
    val category: String,
    val isRetryable: Boolean
)
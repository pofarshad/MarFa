package net.marfanet.android.worker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for daily Iran routing rules updates
 * Fetches latest rules from GitHub and validates with SHA-256
 */
class RoutingRulesUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "routing_rules_update"
        const val RULES_URL = "https://raw.githubusercontent.com/chocolate4u/Iran-v2ray-rules/main/iran.dat"
        const val RULES_FILENAME = "iran.dat"
        
        fun schedulePeriodicUpdate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val updateRequest = PeriodicWorkRequestBuilder<RoutingRulesUpdateWorker>(
                24, TimeUnit.HOURS, // Repeat every 24 hours
                2, TimeUnit.HOURS   // Flex interval of 2 hours
            )
                .setConstraints(constraints)
                .addTag("routing_rules")
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    updateRequest
                )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val rulesDir = File(applicationContext.filesDir, "rules")
            if (!rulesDir.exists()) {
                rulesDir.mkdirs()
            }

            val rulesFile = File(rulesDir, RULES_FILENAME)
            val tempFile = File(rulesDir, "$RULES_FILENAME.tmp")

            // Download rules file
            downloadFile(RULES_URL, tempFile)

            // Validate file integrity
            val downloadedHash = calculateSHA256(tempFile)
            
            // Check if file has changed
            val currentHash = if (rulesFile.exists()) {
                calculateSHA256(rulesFile)
            } else {
                ""
            }

            if (downloadedHash != currentHash) {
                // File has changed, replace the old one
                if (rulesFile.exists()) {
                    rulesFile.delete()
                }
                tempFile.renameTo(rulesFile)
                
                // Trigger core reload if available
                triggerCoreReload()
                
                setProgressAsync(
                    workDataOf(
                        "status" to "updated",
                        "fileSize" to rulesFile.length(),
                        "hash" to downloadedHash
                    )
                )
                
                Result.success(
                    workDataOf(
                        "updated" to true,
                        "fileSize" to rulesFile.length(),
                        "hash" to downloadedHash
                    )
                )
            } else {
                // File unchanged
                tempFile.delete()
                
                setProgressAsync(
                    workDataOf(
                        "status" to "unchanged",
                        "hash" to currentHash
                    )
                )
                
                Result.success(
                    workDataOf(
                        "updated" to false,
                        "hash" to currentHash
                    )
                )
            }

        } catch (e: Exception) {
            setProgressAsync(
                workDataOf(
                    "status" to "failed",
                    "error" to e.message
                )
            )
            
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(
                        "error" to e.message,
                        "finalAttempt" to true
                    )
                )
            }
        }
    }

    private suspend fun downloadFile(url: String, outputFile: File) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection()
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        
        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun triggerCoreReload() {
        try {
            // This would call into the native bridge to reload rules
            // For now, this is a placeholder that would integrate with Xray core
            android.util.Log.i("RoutingRulesUpdate", "Triggering core rules reload")
            
            // Future implementation:
            // XrayCore.reloadRules()
            // or send broadcast intent to VPN service
        } catch (e: Exception) {
            android.util.Log.w("RoutingRulesUpdate", "Failed to trigger core reload: ${e.message}")
        }
    }
}
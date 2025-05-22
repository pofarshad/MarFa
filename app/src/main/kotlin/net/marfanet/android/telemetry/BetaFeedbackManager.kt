package net.marfanet.android.telemetry

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * MarFaNet Beta Feedback & Telemetry Manager
 * Handles crash reporting, performance metrics, and user feedback collection
 */
class BetaFeedbackManager(private val context: Context) {
    
    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Set custom keys for better crash analysis
        crashlytics.setCustomKey("xray_version", "1.8.8")
        crashlytics.setCustomKey("device_api", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
        crashlytics.setCustomKey("app_flavor", if (context.packageName.contains(".beta")) "beta" else "release")
    }
    
    /**
     * Log performance metrics for beta analysis
     */
    fun logPerformanceMetric(metricName: String, value: Double, unit: String) {
        // Firebase Analytics custom event
        analytics.logEvent("performance_metric", bundleOf(
            "metric_name" to metricName,
            "value" to value,
            "unit" to unit,
            "timestamp" to System.currentTimeMillis()
        ))
        
        // Crashlytics custom key for crash correlation
        crashlytics.setCustomKey("last_${metricName}", value.toString())
    }
    
    /**
     * Track VPN connection events for stability analysis
     */
    fun trackConnectionEvent(event: String, duration: Long? = null, errorCode: String? = null) {
        val params = mutableMapOf<String, Any>(
            "event_type" to event,
            "timestamp" to System.currentTimeMillis()
        )
        
        duration?.let { params["duration_ms"] = it }
        errorCode?.let { params["error_code"] = it }
        
        analytics.logEvent("vpn_connection", bundleOf(*params.toList().toTypedArray()))
        
        // Log connection errors to Crashlytics
        if (errorCode != null) {
            crashlytics.recordException(ConnectionException("VPN $event failed: $errorCode"))
        }
    }
    
    /**
     * Open feedback dialog for user reports
     */
    fun openFeedbackDialog() {
        val deviceInfo = buildString {
            appendLine("MarFaNet Beta Feedback")
            appendLine("========================")
            appendLine("App Version: ${getAppVersion()}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine()
            appendLine("Please describe your issue or feedback:")
            appendLine()
        }
        
        // Open GitHub Discussions for feedback
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/marfanet/android/discussions/new?category=beta-feedback&body=${Uri.encode(deviceInfo)}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to email if GitHub not available
            openEmailFeedback(deviceInfo)
        }
    }
    
    /**
     * Report critical crash with enhanced context
     */
    fun reportCriticalCrash(throwable: Throwable, context: String) {
        crashlytics.setCustomKey("crash_context", context)
        crashlytics.setCustomKey("crash_timestamp", System.currentTimeMillis())
        crashlytics.recordException(throwable)
        
        // Also log to Analytics for broader tracking
        analytics.logEvent("critical_crash", bundleOf(
            "crash_type" to throwable.javaClass.simpleName,
            "context" to context
        ))
    }
    
    /**
     * Set user properties for cohort analysis
     */
    fun setUserProperties(isInternalTester: Boolean = false, userGroup: String = "general") {
        crashlytics.setUserId(generateAnonymousId())
        
        analytics.setUserProperty("user_type", if (isInternalTester) "internal" else "beta")
        analytics.setUserProperty("user_group", userGroup)
        analytics.setUserProperty("install_date", getInstallDate())
    }
    
    /**
     * Track beta milestone completion
     */
    fun trackBetaMilestone(milestone: String, success: Boolean, details: String? = null) {
        analytics.logEvent("beta_milestone", bundleOf(
            "milestone" to milestone,
            "success" to success,
            "details" to (details ?: ""),
            "timestamp" to System.currentTimeMillis()
        ))
        
        crashlytics.setCustomKey("last_milestone", milestone)
        crashlytics.setCustomKey("milestone_success", success)
    }
    
    private fun openEmailFeedback(deviceInfo: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:beta@marfanet.com")
            putExtra(Intent.EXTRA_SUBJECT, "MarFaNet Beta Feedback")
            putExtra(Intent.EXTRA_TEXT, deviceInfo)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        context.startActivity(intent)
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun generateAnonymousId(): String {
        val prefs = context.getSharedPreferences("marfanet_beta", Context.MODE_PRIVATE)
        return prefs.getString("anonymous_id", null) ?: run {
            val id = UUID.randomUUID().toString()
            prefs.edit().putString("anonymous_id", id).apply()
            id
        }
    }
    
    private fun getInstallDate(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(packageInfo.firstInstallTime))
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun bundleOf(vararg pairs: Pair<String, Any>) = android.os.Bundle().apply {
        pairs.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}

class ConnectionException(message: String) : Exception(message)
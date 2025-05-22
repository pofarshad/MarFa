package net.marfanet.android.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stats Overlay Permission and Management System
 * RSO-001: Request SYSTEM_ALERT_WINDOW permission gracefully
 */
class StatsOverlayManager(private val context: Context) {
    
    private val _permissionState = MutableStateFlow(OverlayPermissionState.UNKNOWN)
    val permissionState: StateFlow<OverlayPermissionState> = _permissionState.asStateFlow()
    
    private val _overlayState = MutableStateFlow(OverlayUiState())
    val overlayState: StateFlow<OverlayUiState> = _overlayState.asStateFlow()
    
    var isOverlayVisible by mutableStateOf(false)
        private set
    
    /**
     * Check if overlay permission is granted
     */
    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = Settings.canDrawOverlays(context)
            _permissionState.value = if (hasPermission) {
                OverlayPermissionState.GRANTED
            } else {
                OverlayPermissionState.DENIED
            }
            hasPermission
        } else {
            _permissionState.value = OverlayPermissionState.GRANTED
            true
        }
    }
    
    /**
     * Request overlay permission from user
     */
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                _permissionState.value = OverlayPermissionState.REQUESTING
                
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                activity.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
        }
    }
    
    /**
     * Show the stats overlay
     */
    fun showOverlay() {
        if (checkPermission()) {
            isOverlayVisible = true
            _overlayState.value = _overlayState.value.copy(isVisible = true)
        }
    }
    
    /**
     * Hide the stats overlay
     */
    fun hideOverlay() {
        isOverlayVisible = false
        _overlayState.value = _overlayState.value.copy(isVisible = false)
    }
    
    /**
     * Update overlay stats
     */
    fun updateStats(rtt: Long, uploadSpeed: Long, downloadSpeed: Long) {
        _overlayState.value = _overlayState.value.copy(
            rtt = rtt,
            uploadSpeed = uploadSpeed,
            downloadSpeed = downloadSpeed,
            lastUpdate = System.currentTimeMillis()
        )
    }
    
    companion object {
        const val REQUEST_OVERLAY_PERMISSION = 1001
    }
}

/**
 * Overlay permission states
 */
enum class OverlayPermissionState {
    UNKNOWN,
    GRANTED,
    DENIED,
    REQUESTING
}

/**
 * Overlay UI state
 */
data class OverlayUiState(
    val isVisible: Boolean = false,
    val rtt: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val lastUpdate: Long = 0L
) {
    fun formatRtt(): String = "${rtt}ms"
    
    fun formatUploadSpeed(): String {
        return when {
            uploadSpeed < 1024 -> "${uploadSpeed}B/s"
            uploadSpeed < 1024 * 1024 -> "${uploadSpeed / 1024}KB/s"
            else -> "${uploadSpeed / (1024 * 1024)}MB/s"
        }
    }
    
    fun formatDownloadSpeed(): String {
        return when {
            downloadSpeed < 1024 -> "${downloadSpeed}B/s"
            downloadSpeed < 1024 * 1024 -> "${downloadSpeed / 1024}KB/s"
            else -> "${downloadSpeed / (1024 * 1024)}MB/s"
        }
    }
}
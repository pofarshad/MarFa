package net.marfanet.android.stats

/**
 * VPN Statistics Data Model
 * Shared data class for VPN statistics across different collectors
 */
data class VpnStats(
    val rtt: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 100,
    val isLowPowerMode: Boolean = false
) {
    fun isValid(): Boolean = rtt >= 0 && uploadSpeed >= 0 && downloadSpeed >= 0
    
    fun formatRtt(): String = if (rtt >= 0) "${rtt}ms" else "N/A"
    
    fun formatUploadSpeed(): String = formatSpeed(uploadSpeed)
    
    fun formatDownloadSpeed(): String = formatSpeed(downloadSpeed)
    
    private fun formatSpeed(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B/s"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB/s"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB/s"
        else -> "${bytes / (1024 * 1024 * 1024)}GB/s"
    }
}

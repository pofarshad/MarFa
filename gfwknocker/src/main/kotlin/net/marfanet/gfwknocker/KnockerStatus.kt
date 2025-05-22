package net.marfanet.gfwknocker

import org.json.JSONObject

/**
 * Status information for GFW Knocker
 */
data class KnockerStatus(
    val isRunning: Boolean,
    val packetsKnocked: Long,
    val lastKnockTime: Long,
    val successRate: Float = 0.0f,
    val activeTargets: Int = 0,
    val errorMessage: String? = null
) {
    companion object {
        fun fromJson(jsonString: String): KnockerStatus {
            val json = JSONObject(jsonString)
            return KnockerStatus(
                isRunning = json.optBoolean("isRunning", false),
                packetsKnocked = json.optLong("packetsKnocked", 0),
                lastKnockTime = json.optLong("lastKnockTime", 0),
                successRate = json.optDouble("successRate", 0.0).toFloat(),
                activeTargets = json.optInt("activeTargets", 0),
                errorMessage = json.optString("errorMessage", null)
            )
        }
    }
}
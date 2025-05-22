package net.marfanet.gfwknocker

import org.json.JSONObject

/**
 * Configuration class for GFW Knocker
 */
data class KnockerConfig(
    val enabled: Boolean = true,
    val sensitivity: KnockerSensitivity = KnockerSensitivity.MEDIUM,
    val knockInterval: Long = 1000, // milliseconds
    val maxRetries: Int = 3,
    val timeout: Long = 5000, // milliseconds
    val targetHosts: List<String> = listOf(
        "8.8.8.8",
        "1.1.1.1",
        "208.67.222.222"
    ),
    val knockPorts: List<Int> = listOf(53, 80, 443, 8080),
    val detectMethod: DetectionMethod = DetectionMethod.ADAPTIVE
) {
    
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("enabled", enabled)
        json.put("sensitivity", sensitivity.name)
        json.put("knockInterval", knockInterval)
        json.put("maxRetries", maxRetries)
        json.put("timeout", timeout)
        json.put("targetHosts", targetHosts)
        json.put("knockPorts", knockPorts)
        json.put("detectMethod", detectMethod.name)
        return json.toString()
    }
    
    companion object {
        fun fromJsonString(jsonString: String): KnockerConfig {
            val json = JSONObject(jsonString)
            return KnockerConfig(
                enabled = json.optBoolean("enabled", true),
                sensitivity = KnockerSensitivity.valueOf(
                    json.optString("sensitivity", "MEDIUM")
                ),
                knockInterval = json.optLong("knockInterval", 1000),
                maxRetries = json.optInt("maxRetries", 3),
                timeout = json.optLong("timeout", 5000),
                targetHosts = json.optJSONArray("targetHosts")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: listOf("8.8.8.8", "1.1.1.1"),
                knockPorts = json.optJSONArray("knockPorts")?.let { array ->
                    (0 until array.length()).map { array.getInt(it) }
                } ?: listOf(53, 80, 443),
                detectMethod = DetectionMethod.valueOf(
                    json.optString("detectMethod", "ADAPTIVE")
                )
            )
        }
    }
}

enum class KnockerSensitivity {
    LOW,    // Less aggressive knocking
    MEDIUM, // Balanced approach
    HIGH    // More aggressive knocking
}

enum class DetectionMethod {
    TCP_CONNECT,  // TCP connection-based detection
    ICMP_PING,   // ICMP ping-based detection
    DNS_LOOKUP,  // DNS resolution-based detection
    ADAPTIVE     // Adaptive method selection
}
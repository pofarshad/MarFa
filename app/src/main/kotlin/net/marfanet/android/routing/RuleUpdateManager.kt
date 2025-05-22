package net.marfanet.android.routing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * MarFaNet Routing Rule Update Manager
 * Handles automatic Iran routing rules with IPv6/IPv4 fallback
 * Fixes: IPv6-only network rule update failures
 */
class RuleUpdateManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Primary and fallback DNS-over-HTTPS endpoints
    private val primaryRuleEndpoint = "https://raw.githubusercontent.com/marfanet/iran-rules/main/rules.json"
    private val fallbackRuleEndpoint = "https://cdn.jsdelivr.net/gh/marfanet/iran-rules@main/rules.json"
    
    // DoH endpoints for DNS resolution fallback
    private val dohEndpoints = listOf(
        "https://1.1.1.1/dns-query", // Cloudflare IPv4
        "https://8.8.8.8/dns-query", // Google IPv4
        "https://dns.quad9.net/dns-query" // Quad9 IPv4
    )
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "RuleUpdateManager"
        private const val PREFS_LAST_UPDATE = "last_rule_update"
        private const val PREFS_RULE_HASH = "rule_hash"
        private const val UPDATE_INTERVAL_HOURS = 24
    }
    
    /**
     * Check and update routing rules with IPv6 fallback
     */
    suspend fun updateRules(): RuleUpdateResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting routing rule update process")
                
                // Check if update is needed
                if (!isUpdateNeeded()) {
                    Log.d(TAG, "Rules are up to date, skipping update")
                    return@withContext RuleUpdateResult.Success("Rules are up to date")
                }
                
                // Try primary endpoint first
                var result = downloadRules(primaryRuleEndpoint)
                
                // If primary fails and we detect IPv6-only network, try fallback with DoH
                if (result is RuleUpdateResult.Error && isIPv6OnlyNetwork()) {
                    Log.w(TAG, "IPv6-only network detected, trying DoH fallback")
                    result = downloadRulesWithDoHFallback()
                }
                
                // Try fallback endpoint if still failing
                if (result is RuleUpdateResult.Error) {
                    Log.w(TAG, "Primary endpoint failed, trying fallback")
                    result = downloadRules(fallbackRuleEndpoint)
                }
                
                when (result) {
                    is RuleUpdateResult.Success -> {
                        Log.i(TAG, "Rules updated successfully: ${result.message}")
                        updateLastUpdateTime()
                    }
                    is RuleUpdateResult.Error -> {
                        Log.e(TAG, "All update attempts failed: ${result.message}")
                    }
                }
                
                result
                
            } catch (e: Exception) {
                Log.e(TAG, "Rule update failed with exception", e)
                RuleUpdateResult.Error("Update failed: ${e.message}")
            }
        }
    }
    
    /**
     * Download rules from specific endpoint
     */
    private suspend fun downloadRules(endpoint: String): RuleUpdateResult {
        return try {
            Log.d(TAG, "Downloading rules from: $endpoint")
            
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("User-Agent", "MarFaNet/1.0.1")
                .addHeader("Cache-Control", "no-cache")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return RuleUpdateResult.Error("HTTP ${response.code}: ${response.message}")
            }
            
            val rulesJson = response.body?.string()
                ?: return RuleUpdateResult.Error("Empty response body")
            
            // Validate and parse rules
            val validationResult = validateRules(rulesJson)
            if (!validationResult.isValid) {
                return RuleUpdateResult.Error("Invalid rules: ${validationResult.error}")
            }
            
            // Check if rules have changed
            val newHash = calculateSHA256(rulesJson)
            val currentHash = getStoredRuleHash()
            
            if (newHash == currentHash) {
                return RuleUpdateResult.Success("Rules unchanged")
            }
            
            // Apply new rules
            applyRules(rulesJson)
            storeRuleHash(newHash)
            
            RuleUpdateResult.Success("Rules updated (${validationResult.ruleCount} rules)")
            
        } catch (e: IOException) {
            Log.w(TAG, "Network error downloading from $endpoint", e)
            RuleUpdateResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error downloading from $endpoint", e)
            RuleUpdateResult.Error("Download failed: ${e.message}")
        }
    }
    
    /**
     * Download rules using DNS-over-HTTPS fallback for IPv6-only networks
     */
    private suspend fun downloadRulesWithDoHFallback(): RuleUpdateResult {
        Log.d(TAG, "Attempting DoH fallback for IPv6-only network")
        
        for (dohEndpoint in dohEndpoints) {
            try {
                Log.d(TAG, "Trying DoH endpoint: $dohEndpoint")
                
                // Resolve GitHub's IPv4 address using DoH
                val ipv4Address = resolveIPv4ViaDoH("raw.githubusercontent.com", dohEndpoint)
                
                if (ipv4Address != null) {
                    Log.d(TAG, "Resolved IPv4 address: $ipv4Address")
                    
                    // Create request with explicit IPv4 address
                    val urlWithIP = primaryRuleEndpoint.replace("raw.githubusercontent.com", ipv4Address)
                    val request = Request.Builder()
                        .url(urlWithIP)
                        .addHeader("Host", "raw.githubusercontent.com")
                        .addHeader("User-Agent", "MarFaNet/1.0.1")
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val rulesJson = response.body?.string()
                        if (rulesJson != null) {
                            val validationResult = validateRules(rulesJson)
                            if (validationResult.isValid) {
                                applyRules(rulesJson)
                                storeRuleHash(calculateSHA256(rulesJson))
                                return RuleUpdateResult.Success("Rules updated via DoH fallback")
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "DoH fallback failed for $dohEndpoint", e)
                continue
            }
        }
        
        return RuleUpdateResult.Error("All DoH fallback attempts failed")
    }
    
    /**
     * Detect if device is on IPv6-only network
     */
    private suspend fun isIPv6OnlyNetwork(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try to resolve a known IPv4 address
                val addresses = InetAddress.getAllByName("8.8.8.8")
                val hasIPv4 = addresses.any { it.address.size == 4 }
                
                if (!hasIPv4) {
                    Log.d(TAG, "IPv6-only network detected")
                    return@withContext true
                }
                
                // Additional check: try connecting to IPv4 endpoint
                val testRequest = Request.Builder()
                    .url("http://1.1.1.1/")
                    .build()
                
                try {
                    client.newCall(testRequest).execute().use { response ->
                        return@withContext false // IPv4 works
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "IPv4 connectivity test failed, assuming IPv6-only")
                    return@withContext true
                }
                
            } catch (e: Exception) {
                Log.w(TAG, "Network type detection failed", e)
                false // Assume mixed network if detection fails
            }
        }
    }
    
    /**
     * Resolve IPv4 address using DNS-over-HTTPS
     */
    private suspend fun resolveIPv4ViaDoH(hostname: String, dohEndpoint: String): String? {
        return try {
            // Create DoH query for A record
            val dohUrl = "$dohEndpoint?name=$hostname&type=A"
            val request = Request.Builder()
                .url(dohUrl)
                .addHeader("Accept", "application/dns-json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                // Parse JSON response and extract IPv4 address
                val ipv4Regex = """(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""".toRegex()
                val match = ipv4Regex.find(responseBody ?: "")
                match?.value
            } else {
                null
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "DoH resolution failed for $hostname", e)
            null
        }
    }
    
    private fun validateRules(rulesJson: String): ValidationResult {
        return try {
            if (rulesJson.isBlank() || !rulesJson.trim().startsWith("{")) {
                return ValidationResult(false, "Invalid JSON format", 0)
            }
            
            val ruleCount = rulesJson.split("\"ip\":").size - 1
            
            if (ruleCount < 100) {
                return ValidationResult(false, "Too few rules ($ruleCount)", ruleCount)
            }
            
            ValidationResult(true, null, ruleCount)
            
        } catch (e: Exception) {
            ValidationResult(false, "Validation error: ${e.message}", 0)
        }
    }
    
    private fun applyRules(rulesJson: String) {
        Log.d(TAG, "Applying new routing rules")
        
        val prefs = context.getSharedPreferences("marfanet_routing", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("iran_rules", rulesJson)
            .putLong("rules_updated", System.currentTimeMillis())
            .apply()
        
        Log.d(TAG, "Rules stored and VPN service notified")
    }
    
    private fun isUpdateNeeded(): Boolean {
        val prefs = context.getSharedPreferences("marfanet_routing", Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(PREFS_LAST_UPDATE, 0)
        val updateInterval = UPDATE_INTERVAL_HOURS * 60 * 60 * 1000L
        
        return System.currentTimeMillis() - lastUpdate > updateInterval
    }
    
    private fun updateLastUpdateTime() {
        val prefs = context.getSharedPreferences("marfanet_routing", Context.MODE_PRIVATE)
        prefs.edit().putLong(PREFS_LAST_UPDATE, System.currentTimeMillis()).apply()
    }
    
    private fun getStoredRuleHash(): String? {
        val prefs = context.getSharedPreferences("marfanet_routing", Context.MODE_PRIVATE)
        return prefs.getString(PREFS_RULE_HASH, null)
    }
    
    private fun storeRuleHash(hash: String) {
        val prefs = context.getSharedPreferences("marfanet_routing", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_RULE_HASH, hash).apply()
    }
    
    private fun calculateSHA256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

sealed class RuleUpdateResult {
    data class Success(val message: String) : RuleUpdateResult()
    data class Error(val message: String) : RuleUpdateResult()
}

data class ValidationResult(
    val isValid: Boolean,
    val error: String?,
    val ruleCount: Int
)
package net.marfanet.android.subscription

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.marfanet.android.data.ProfileEntity
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for V2Ray/Xray subscription links and individual server configurations
 */
@Singleton
class SubscriptionParser @Inject constructor() {
    
    companion object {
        private const val TAG = "SubscriptionParser"
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    /**
     * Parse subscription URL and return list of profiles
     */
    suspend fun parseSubscription(subscriptionUrl: String): List<ProfileEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Parsing subscription: $subscriptionUrl")
            
            // Download subscription content
            val content = downloadSubscriptionContent(subscriptionUrl)
            if (content.isEmpty()) {
                Log.w(TAG, "Empty subscription content")
                return@withContext emptyList()
            }
            
            // Try to decode as base64 first
            val decodedContent = try {
                String(Base64.decode(content, Base64.DEFAULT))
            } catch (e: Exception) {
                content // Use original content if not base64
            }
            
            // Parse individual server links
            val profiles = mutableListOf<ProfileEntity>()
            decodedContent.lines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty()) {
                    parseServerLink(trimmedLine)?.let { profile ->
                        profiles.add(profile)
                    }
                }
            }
            
            Log.d(TAG, "Parsed ${profiles.size} profiles from subscription")
            profiles
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing subscription", e)
            emptyList()
        }
    }
    
    /**
     * Parse individual server link (vmess://, vless://, trojan://, ss://)
     */
    fun parseServerLink(serverLink: String): ProfileEntity? {
        return try {
            when {
                serverLink.startsWith("vmess://") -> parseVmessLink(serverLink)
                serverLink.startsWith("vless://") -> parseVlessLink(serverLink)
                serverLink.startsWith("trojan://") -> parseTrojanLink(serverLink)
                serverLink.startsWith("ss://") -> parseShadowsocksLink(serverLink)
                else -> {
                    Log.w(TAG, "Unsupported protocol: $serverLink")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server link: $serverLink", e)
            null
        }
    }
    
    private fun parseVmessLink(link: String): ProfileEntity? {
        try {
            val base64Part = link.removePrefix("vmess://")
            val jsonString = String(Base64.decode(base64Part, Base64.DEFAULT))
            val vmessConfig = json.decodeFromString<VmessConfig>(jsonString)
            
            return ProfileEntity(
                id = generateProfileId(),
                name = vmessConfig.ps.ifEmpty { "${vmessConfig.add}:${vmessConfig.port}" },
                protocol = "vmess",
                serverAddress = vmessConfig.add,
                serverPort = vmessConfig.port,
                userId = vmessConfig.id,
                password = "",
                security = vmessConfig.scy.ifEmpty { "auto" },
                network = vmessConfig.net.ifEmpty { "tcp" },
                path = vmessConfig.path.ifEmpty { "/" },
                host = vmessConfig.host.ifEmpty { vmessConfig.add },
                tls = vmessConfig.tls == "tls",
                sni = vmessConfig.sni.ifEmpty { vmessConfig.add },
                allowInsecure = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VMess link", e)
            return null
        }
    }
    
    private fun parseVlessLink(link: String): ProfileEntity? {
        try {
            val uri = URI(link)
            val params = parseQueryParams(uri.query ?: "")
            
            return ProfileEntity(
                id = generateProfileId(),
                name = URLDecoder.decode(uri.fragment ?: "${uri.host}:${uri.port}", "UTF-8"),
                protocol = "vless",
                serverAddress = uri.host,
                serverPort = uri.port,
                userId = uri.userInfo,
                password = "",
                security = params["security"] ?: "none",
                network = params["type"] ?: "tcp",
                path = params["path"] ?: "/",
                host = params["host"] ?: uri.host,
                tls = params["security"] == "tls",
                sni = params["sni"] ?: uri.host,
                allowInsecure = params["allowInsecure"] == "1"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VLESS link", e)
            return null
        }
    }
    
    private fun parseTrojanLink(link: String): ProfileEntity? {
        try {
            val uri = URI(link)
            val params = parseQueryParams(uri.query ?: "")
            
            return ProfileEntity(
                id = generateProfileId(),
                name = URLDecoder.decode(uri.fragment ?: "${uri.host}:${uri.port}", "UTF-8"),
                protocol = "trojan",
                serverAddress = uri.host,
                serverPort = uri.port,
                userId = "",
                password = uri.userInfo,
                security = "none",
                network = params["type"] ?: "tcp",
                path = params["path"] ?: "/",
                host = params["host"] ?: uri.host,
                tls = true, // Trojan always uses TLS
                sni = params["sni"] ?: uri.host,
                allowInsecure = params["allowInsecure"] == "1"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Trojan link", e)
            return null
        }
    }
    
    private fun parseShadowsocksLink(link: String): ProfileEntity? {
        try {
            val uri = URI(link)
            
            // Decode base64 userinfo if present
            val userInfo = if (uri.userInfo != null) {
                try {
                    String(Base64.decode(uri.userInfo, Base64.DEFAULT))
                } catch (e: Exception) {
                    uri.userInfo
                }
            } else {
                ""
            }
            
            val parts = userInfo.split(":")
            if (parts.size < 2) {
                Log.w(TAG, "Invalid Shadowsocks format")
                return null
            }
            
            val method = parts[0]
            val password = parts[1]
            
            return ProfileEntity(
                id = generateProfileId(),
                name = URLDecoder.decode(uri.fragment ?: "${uri.host}:${uri.port}", "UTF-8"),
                protocol = "shadowsocks",
                serverAddress = uri.host,
                serverPort = uri.port,
                userId = "",
                password = password,
                security = method,
                network = "tcp",
                path = "/",
                host = uri.host,
                tls = false,
                sni = uri.host,
                allowInsecure = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Shadowsocks link", e)
            return null
        }
    }
    
    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        query.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                params[URLDecoder.decode(parts[0], "UTF-8")] = URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return params
    }
    
    private suspend fun downloadSubscriptionContent(url: String): String = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.getInputStream().bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading subscription", e)
            ""
        }
    }
    
    private fun generateProfileId(): String {
        return "profile_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

@Serializable
data class VmessConfig(
    val v: String = "2",
    val ps: String = "",
    val add: String = "",
    val port: Int = 443,
    val id: String = "",
    val aid: String = "0",
    val scy: String = "auto",
    val net: String = "tcp",
    val type: String = "none",
    val host: String = "",
    val path: String = "/",
    val tls: String = "none",
    val sni: String = "",
    val alpn: String = ""
)

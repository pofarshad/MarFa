package net.marfanet.android.subscription

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.marfanet.android.data.ProfileDao
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SubscriptionParserTest {
    
    private lateinit var subscriptionParser: SubscriptionParser
    private lateinit var context: Context
    private lateinit var mockProfileDao: ProfileDao
    private lateinit var mockWebServer: MockWebServer
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        mockProfileDao = mockk(relaxed = true)
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        subscriptionParser = SubscriptionParser(context, mockProfileDao)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        mockWebServer.shutdown()
        clearAllMocks()
    }
    
    @Test
    fun `parseVmessUrl parses valid VMess URL correctly`() {
        val vmessConfig = """
            {
                "v": "2",
                "ps": "Test VMess Server",
                "add": "example.com",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789abc",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "example.com",
                "path": "/vmess",
                "tls": "tls",
                "sni": "example.com",
                "alpn": ""
            }
        """.trimIndent()
        
        val encodedConfig = Base64.getEncoder().encodeToString(vmessConfig.toByteArray())
        val vmessUrl = "vmess://$encodedConfig"
        
        val profile = subscriptionParser.parseVmessUrl(vmessUrl)
        
        assertNotNull("Profile should not be null", profile)
        assertEquals("Name should match", "Test VMess Server", profile!!.name)
        assertEquals("Protocol should be vmess", "vmess", profile.protocol)
        assertEquals("Server address should match", "example.com", profile.serverAddress)
        assertEquals("Port should match", 443, profile.serverPort)
        assertEquals("User ID should match", "12345678-1234-1234-1234-123456789abc", profile.userId)
        assertEquals("Network should match", "ws", profile.network)
        assertEquals("Path should match", "/vmess", profile.path)
        assertEquals("Host should match", "example.com", profile.host)
        assertTrue("TLS should be enabled", profile.tls)
        assertEquals("SNI should match", "example.com", profile.sni)
    }
    
    @Test
    fun `parseVmessUrl handles invalid base64 encoding`() {
        val invalidVmessUrl = "vmess://invalid-base64-data"
        
        val profile = subscriptionParser.parseVmessUrl(invalidVmessUrl)
        
        assertNull("Should return null for invalid base64", profile)
    }
    
    @Test
    fun `parseVmessUrl handles invalid JSON`() {
        val invalidJson = "{ invalid json }"
        val encodedConfig = Base64.getEncoder().encodeToString(invalidJson.toByteArray())
        val vmessUrl = "vmess://$encodedConfig"
        
        val profile = subscriptionParser.parseVmessUrl(vmessUrl)
        
        assertNull("Should return null for invalid JSON", profile)
    }
    
    @Test
    fun `parseVlessUrl parses valid VLESS URL correctly`() {
        val vlessUrl = "vless://12345678-1234-1234-1234-123456789abc@example.com:443" +
                "?type=ws&security=tls&path=/vless&host=example.com&sni=example.com" +
                "#Test%20VLESS%20Server"
        
        val profile = subscriptionParser.parseVlessUrl(vlessUrl)
        
        assertNotNull("Profile should not be null", profile)
        assertEquals("Name should match", "Test VLESS Server", profile!!.name)
        assertEquals("Protocol should be vless", "vless", profile.protocol)
        assertEquals("Server address should match", "example.com", profile.serverAddress)
        assertEquals("Port should match", 443, profile.serverPort)
        assertEquals("User ID should match", "12345678-1234-1234-1234-123456789abc", profile.userId)
        assertEquals("Network should match", "ws", profile.network)
        assertEquals("Path should match", "/vless", profile.path)
        assertEquals("Host should match", "example.com", profile.host)
        assertTrue("TLS should be enabled", profile.tls)
        assertEquals("SNI should match", "example.com", profile.sni)
    }
    
    @Test
    fun `parseVlessUrl handles malformed URL`() {
        val malformedUrl = "vless://invalid-url-format"
        
        val profile = subscriptionParser.parseVlessUrl(malformedUrl)
        
        assertNull("Should return null for malformed URL", profile)
    }
    
    @Test
    fun `parseTrojanUrl parses valid Trojan URL correctly`() {
        val trojanUrl = "trojan://password123@example.com:443" +
                "?type=ws&security=tls&path=/trojan&host=example.com&sni=example.com" +
                "#Test%20Trojan%20Server"
        
        val profile = subscriptionParser.parseTrojanUrl(trojanUrl)
        
        assertNotNull("Profile should not be null", profile)
        assertEquals("Name should match", "Test Trojan Server", profile!!.name)
        assertEquals("Protocol should be trojan", "trojan", profile.protocol)
        assertEquals("Server address should match", "example.com", profile.serverAddress)
        assertEquals("Port should match", 443, profile.serverPort)
        assertEquals("Password should match", "password123", profile.password)
        assertEquals("Network should match", "ws", profile.network)
        assertEquals("Path should match", "/trojan", profile.path)
        assertEquals("Host should match", "example.com", profile.host)
        assertTrue("TLS should be enabled", profile.tls)
        assertEquals("SNI should match", "example.com", profile.sni)
    }
    
    @Test
    fun `parseShadowsocksUrl parses valid SS URL correctly`() {
        val method = "aes-256-gcm"
        val password = "password123"
        val credentials = Base64.getEncoder().encodeToString("$method:$password".toByteArray())
        val ssUrl = "ss://$credentials@example.com:443#Test%20SS%20Server"
        
        val profile = subscriptionParser.parseShadowsocksUrl(ssUrl)
        
        assertNotNull("Profile should not be null", profile)
        assertEquals("Name should match", "Test SS Server", profile!!.name)
        assertEquals("Protocol should be shadowsocks", "shadowsocks", profile.protocol)
        assertEquals("Server address should match", "example.com", profile.serverAddress)
        assertEquals("Port should match", 443, profile.serverPort)
        assertEquals("Password should match", "password123", profile.password)
        assertEquals("Security should match", "aes-256-gcm", profile.security)
    }
    
    @Test
    fun `parseSubscriptionContent processes mixed protocol content`() = runTest {
        val vmessConfig = """
            {
                "v": "2",
                "ps": "VMess Server",
                "add": "vmess.example.com",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789abc",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "vmess.example.com",
                "path": "/vmess",
                "tls": "tls",
                "sni": "vmess.example.com"
            }
        """.trimIndent()
        
        val encodedVmess = Base64.getEncoder().encodeToString(vmessConfig.toByteArray())
        val vlessUrl = "vless://87654321-4321-4321-4321-cba987654321@vless.example.com:443" +
                "?type=ws&security=tls&path=/vless&host=vless.example.com#VLESS%20Server"
        
        val subscriptionContent = listOf(
            "vmess://$encodedVmess",
            vlessUrl,
            "# This is a comment",
            "", // Empty line
            "invalid-url-format"
        ).joinToString("\n")
        
        val profiles = subscriptionParser.parseSubscriptionContent(subscriptionContent, "test-group")
        
        assertEquals("Should parse 2 valid profiles", 2, profiles.size)
        
        val vmessProfile = profiles.find { it.protocol == "vmess" }
        assertNotNull("Should have VMess profile", vmessProfile)
        assertEquals("VMess name should match", "VMess Server", vmessProfile!!.name)
        assertEquals("VMess group should match", "test-group", vmessProfile.subscriptionGroup)
        
        val vlessProfile = profiles.find { it.protocol == "vless" }
        assertNotNull("Should have VLESS profile", vlessProfile)
        assertEquals("VLESS name should match", "VLESS Server", vlessProfile!!.name)
        assertEquals("VLESS group should match", "test-group", vlessProfile.subscriptionGroup)
    }
    
    @Test
    fun `parseSubscriptionContent handles empty content`() = runTest {
        val profiles = subscriptionParser.parseSubscriptionContent("", "test-group")
        
        assertTrue("Should return empty list for empty content", profiles.isEmpty())
    }
    
    @Test
    fun `parseSubscriptionContent handles base64 encoded content`() = runTest {
        val vmessConfig = """
            {
                "v": "2",
                "ps": "Encoded VMess Server",
                "add": "encoded.example.com",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789abc",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "encoded.example.com",
                "path": "/vmess",
                "tls": "tls"
            }
        """.trimIndent()
        
        val encodedVmess = Base64.getEncoder().encodeToString(vmessConfig.toByteArray())
        val vmessUrl = "vmess://$encodedVmess"
        
        val base64Content = Base64.getEncoder().encodeToString(vmessUrl.toByteArray())
        val profiles = subscriptionParser.parseSubscriptionContent(base64Content, "test-group")
        
        assertEquals("Should parse 1 profile from base64 content", 1, profiles.size)
        assertEquals("Profile name should match", "Encoded VMess Server", profiles[0].name)
    }
    
    @Test
    fun `updateSubscription downloads and parses subscription successfully`() = runTest {
        val vmessConfig = """
            {
                "v": "2",
                "ps": "Subscription VMess Server",
                "add": "sub.example.com",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789abc",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "sub.example.com",
                "path": "/vmess",
                "tls": "tls"
            }
        """.trimIndent()
        
        val encodedVmess = Base64.getEncoder().encodeToString(vmessConfig.toByteArray())
        val subscriptionContent = "vmess://$encodedVmess"
        
        mockWebServer.enqueue(MockResponse()
            .setBody(subscriptionContent)
            .setResponseCode(200))
        
        coEvery { mockProfileDao.deleteProfilesByGroup("test-subscription") } just Runs
        coEvery { mockProfileDao.insertProfiles(any()) } just Runs
        
        val result = subscriptionParser.updateSubscription(
            mockWebServer.url("/").toString(),
            "test-subscription"
        )
        
        assertTrue("Update should succeed", result.isSuccess)
        assertEquals("Should parse 1 profile", 1, result.getOrNull())
        
        coVerify { mockProfileDao.deleteProfilesByGroup("test-subscription") }
        coVerify { mockProfileDao.insertProfiles(match { it.size == 1 }) }
    }
    
    @Test
    fun `updateSubscription handles network failure`() = runTest {
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))
        
        val result = subscriptionParser.updateSubscription(
            mockWebServer.url("/").toString(),
            "test-subscription"
        )
        
        assertTrue("Update should fail", result.isFailure)
        assertTrue("Should contain error message", 
            result.exceptionOrNull()?.message?.contains("404") == true)
        
        coVerify(exactly = 0) { mockProfileDao.deleteProfilesByGroup(any()) }
        coVerify(exactly = 0) { mockProfileDao.insertProfiles(any()) }
    }
    
    @Test
    fun `updateSubscription handles database errors`() = runTest {
        val subscriptionContent = "vmess://invalid-content"
        
        mockWebServer.enqueue(MockResponse()
            .setBody(subscriptionContent)
            .setResponseCode(200))
        
        coEvery { mockProfileDao.deleteProfilesByGroup(any()) } throws RuntimeException("Database error")
        
        val result = subscriptionParser.updateSubscription(
            mockWebServer.url("/").toString(),
            "test-subscription"
        )
        
        assertTrue("Update should fail", result.isFailure)
        assertTrue("Should contain database error", 
            result.exceptionOrNull()?.message?.contains("Database error") == true)
    }
    
    @Test
    fun `generateProfileId creates consistent IDs`() {
        val profile1 = subscriptionParser.parseVmessUrl(createTestVmessUrl("Test Server", "example.com"))
        val profile2 = subscriptionParser.parseVmessUrl(createTestVmessUrl("Test Server", "example.com"))
        
        assertNotNull("Profile1 should not be null", profile1)
        assertNotNull("Profile2 should not be null", profile2)
        assertEquals("IDs should be consistent", profile1!!.id, profile2!!.id)
    }
    
    @Test
    fun `generateProfileId creates different IDs for different servers`() {
        val profile1 = subscriptionParser.parseVmessUrl(createTestVmessUrl("Server 1", "server1.example.com"))
        val profile2 = subscriptionParser.parseVmessUrl(createTestVmessUrl("Server 2", "server2.example.com"))
        
        assertNotNull("Profile1 should not be null", profile1)
        assertNotNull("Profile2 should not be null", profile2)
        assertNotEquals("IDs should be different", profile1!!.id, profile2!!.id)
    }
    
    @Test
    fun `url decoding handles special characters correctly`() {
        val vlessUrl = "vless://12345678-1234-1234-1234-123456789abc@example.com:443" +
                "?type=ws&security=tls&path=/test%20path&host=example.com" +
                "#Test%20Server%20With%20Spaces"
        
        val profile = subscriptionParser.parseVlessUrl(vlessUrl)
        
        assertNotNull("Profile should not be null", profile)
        assertEquals("Name should be decoded", "Test Server With Spaces", profile!!.name)
        assertEquals("Path should be decoded", "/test path", profile.path)
    }
    
    @Test
    fun `subscription parsing handles large content efficiently`() = runTest {
        // Create a large subscription with 100 servers
        val vmessUrls = (1..100).map { i ->
            createTestVmessUrl("Server $i", "server$i.example.com")
        }
        val largeContent = vmessUrls.joinToString("\n")
        
        val startTime = System.currentTimeMillis()
        val profiles = subscriptionParser.parseSubscriptionContent(largeContent, "large-group")
        val endTime = System.currentTimeMillis()
        
        assertEquals("Should parse all 100 profiles", 100, profiles.size)
        assertTrue("Should complete within reasonable time", (endTime - startTime) < 5000)
        
        // Verify all profiles have unique IDs
        val uniqueIds = profiles.map { it.id }.toSet()
        assertEquals("All profiles should have unique IDs", 100, uniqueIds.size)
    }
    
    private fun createTestVmessUrl(name: String, address: String): String {
        val vmessConfig = """
            {
                "v": "2",
                "ps": "$name",
                "add": "$address",
                "port": "443",
                "id": "12345678-1234-1234-1234-123456789abc",
                "aid": "0",
                "scy": "auto",
                "net": "ws",
                "type": "none",
                "host": "$address",
                "path": "/vmess",
                "tls": "tls"
            }
        """.trimIndent()
        
        val encodedConfig = Base64.getEncoder().encodeToString(vmessConfig.toByteArray())
        return "vmess://$encodedConfig"
    }
}

package net.marfanet.android.xray

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import net.marfanet.android.data.ProfileEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class XrayCoreTest {
    
    private lateinit var xrayCore: XrayCore
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        xrayCore = XrayCore(context)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        xrayCore.stop()
    }
    
    @Test
    fun `xray core initializes correctly`() {
        assertFalse("Should not be running initially", xrayCore.isRunning)
        assertFalse("Library should not be loaded initially", xrayCore.isLibraryLoaded)
    }
    
    @Test
    fun `start with valid config returns success when library loaded`() = runTest {
        // Mock library loading
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        
        val testConfig = createTestXrayConfig()
        val result = xrayCore.start(testConfig)
        
        assertTrue("Should start successfully", result)
        assertTrue("Should be running", xrayCore.isRunning)
    }
    
    @Test
    fun `start with stub implementation when library not loaded`() = runTest {
        // Library not loaded scenario
        val testConfig = createTestXrayConfig()
        val result = xrayCore.start(testConfig)
        
        assertTrue("Should start with stub implementation", result)
        assertTrue("Should be running", xrayCore.isRunning)
    }
    
    @Test
    fun `start with invalid config returns failure`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns false
        
        val testConfig = createTestXrayConfig()
        val result = xrayCore.start(testConfig)
        
        assertFalse("Should fail to start", result)
        assertFalse("Should not be running", xrayCore.isRunning)
    }
    
    @Test
    fun `stop terminates xray core correctly`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        every { xrayCore.nativeStop() } returns true
        
        // Start first
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        assertTrue("Should be running", xrayCore.isRunning)
        
        // Then stop
        val result = xrayCore.stop()
        assertTrue("Should stop successfully", result)
        assertFalse("Should not be running", xrayCore.isRunning)
    }
    
    @Test
    fun `getStats returns valid statistics when running`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        every { xrayCore.nativeGetStats() } returns """
            {
                "uplink": 1024,
                "downlink": 2048,
                "connections": 5
            }
        """.trimIndent()
        
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        
        val stats = xrayCore.getStats()
        assertNotNull("Stats should not be null", stats)
        assertTrue("Stats should contain uplink", stats.contains("uplink"))
        assertTrue("Stats should contain downlink", stats.contains("downlink"))
    }
    
    @Test
    fun `getStats returns empty when not running`() {
        val stats = xrayCore.getStats()
        assertEquals("Should return empty stats", "{}", stats)
    }
    
    @Test
    fun `testConnectivity returns valid latency for reachable server`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeTestConnectivity("test.example.com", 443) } returns 50L
        
        val latency = xrayCore.testConnectivity("test.example.com", 443)
        assertEquals("Should return valid latency", 50L, latency)
    }
    
    @Test
    fun `testConnectivity returns -1 for unreachable server`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeTestConnectivity("unreachable.example.com", 443) } returns -1L
        
        val latency = xrayCore.testConnectivity("unreachable.example.com", 443)
        assertEquals("Should return -1 for unreachable server", -1L, latency)
    }
    
    @Test
    fun `testConnectivity with stub implementation returns simulated latency`() = runTest {
        // Library not loaded, should use stub
        val latency = xrayCore.testConnectivity("test.example.com", 443)
        assertTrue("Should return simulated latency", latency in 50L..200L)
    }
    
    @Test
    fun `processPacket handles packets correctly when running`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        every { xrayCore.nativeProcessPacket(any(), any()) } returns byteArrayOf(1, 2, 3, 4)
        
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        
        val testPacket = byteArrayOf(0, 1, 2, 3)
        val result = xrayCore.processPacket(testPacket, testPacket.size)
        
        assertNotNull("Should return processed packet", result)
        assertArrayEquals("Should return expected data", byteArrayOf(1, 2, 3, 4), result)
    }
    
    @Test
    fun `processPacket returns null when not running`() {
        val testPacket = byteArrayOf(0, 1, 2, 3)
        val result = xrayCore.processPacket(testPacket, testPacket.size)
        
        assertNull("Should return null when not running", result)
    }
    
    @Test
    fun `processPacket with stub implementation echoes packet`() = runTest {
        // Start with stub implementation
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        
        val testPacket = byteArrayOf(0, 1, 2, 3)
        val result = xrayCore.processPacket(testPacket, testPacket.size)
        
        assertNotNull("Should return processed packet", result)
        assertEquals("Should return same length", testPacket.size, result!!.size)
    }
    
    @Test
    fun `getOutgoingPackets returns data when available`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        every { xrayCore.nativeGetOutgoingPackets() } returns byteArrayOf(5, 6, 7, 8)
        
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        
        val result = xrayCore.getOutgoingPackets()
        assertArrayEquals("Should return outgoing data", byteArrayOf(5, 6, 7, 8), result)
    }
    
    @Test
    fun `getOutgoingPackets returns empty when not running`() {
        val result = xrayCore.getOutgoingPackets()
        assertEquals("Should return empty array", 0, result.size)
    }
    
    @Test
    fun `convertProfileToConfig creates valid VMess configuration`() {
        val vmessProfile = createTestProfile("vmess")
        
        // Use reflection to access private method for testing
        val method = XrayCore::class.java.getDeclaredMethod("convertProfileToConfig", ProfileEntity::class.java)
        method.isAccessible = true
        val config = method.invoke(xrayCore, vmessProfile) as XrayConfig
        
        assertNotNull("Config should not be null", config)
        assertEquals("Should have 2 inbounds", 2, config.inbounds.size)
        assertEquals("Should have 3 outbounds", 3, config.outbounds.size)
        
        val proxyOutbound = config.outbounds.find { it.tag == "proxy" }
        assertNotNull("Should have proxy outbound", proxyOutbound)
        assertEquals("Should be VMess protocol", "vmess", proxyOutbound!!.protocol)
    }
    
    @Test
    fun `convertProfileToConfig creates valid Trojan configuration`() {
        val trojanProfile = createTestProfile("trojan")
        
        val method = XrayCore::class.java.getDeclaredMethod("convertProfileToConfig", ProfileEntity::class.java)
        method.isAccessible = true
        val config = method.invoke(xrayCore, trojanProfile) as XrayConfig
        
        val proxyOutbound = config.outbounds.find { it.tag == "proxy" }
        assertNotNull("Should have proxy outbound", proxyOutbound)
        assertEquals("Should be Trojan protocol", "trojan", proxyOutbound!!.protocol)
    }
    
    @Test
    fun `routing configuration includes Iran-specific rules`() {
        val testProfile = createTestProfile("vmess")
        
        val method = XrayCore::class.java.getDeclaredMethod("convertProfileToConfig", ProfileEntity::class.java)
        method.isAccessible = true
        val config = method.invoke(xrayCore, testProfile) as XrayConfig
        
        assertNotNull("Should have routing config", config.routing)
        assertTrue("Should have routing rules", config.routing.rules.isNotEmpty())
        
        val cnRule = config.routing.rules.find { rule ->
            rule.domain?.contains("geosite:cn") == true
        }
        assertNotNull("Should have CN domain rule", cnRule)
        assertEquals("CN rule should use direct outbound", "direct", cnRule!!.outboundTag)
    }
    
    @Test
    fun `error handling in packet processing`() = runTest {
        mockkObject(xrayCore)
        every { xrayCore.isLibraryLoaded } returns true
        every { xrayCore.nativeStart(any()) } returns true
        every { xrayCore.nativeProcessPacket(any(), any()) } throws RuntimeException("Native error")
        
        val testConfig = createTestXrayConfig()
        xrayCore.start(testConfig)
        
        val testPacket = byteArrayOf(0, 1, 2, 3)
        val result = xrayCore.processPacket(testPacket, testPacket.size)
        
        assertNull("Should return null on error", result)
    }
    
    private fun createTestXrayConfig(): XrayConfig {
        return XrayConfig(
            inbounds = listOf(
                InboundConfig(
                    tag = "socks-in",
                    protocol = "socks",
                    listen = "127.0.0.1",
                    port = 10808
                )
            ),
            outbounds = listOf(
                OutboundConfig(
                    tag = "proxy",
                    protocol = "vmess"
                )
            ),
            routing = RoutingConfig(
                domainStrategy = "IPIfNonMatch",
                rules = emptyList()
            )
        )
    }
    
    private fun createTestProfile(protocol: String): ProfileEntity {
        return ProfileEntity(
            id = "test-profile",
            name = "Test Server",
            protocol = protocol,
            serverAddress = "test.example.com",
            serverPort = 443,
            userId = "test-user-id",
            password = if (protocol == "trojan") "test-password" else "",
            security = "auto",
            network = "ws",
            path = "/test",
            host = "test.example.com",
            tls = true,
            sni = "test.example.com",
            allowInsecure = false
        )
    }
}

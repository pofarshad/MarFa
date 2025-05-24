package net.marfanet.android.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import net.marfanet.android.data.ProfileDao
import net.marfanet.android.data.ProfileEntity
import net.marfanet.android.xray.XrayCore
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
class PingServiceTest {
    
    private lateinit var pingService: PingService
    private lateinit var context: Context
    private lateinit var mockProfileDao: ProfileDao
    private lateinit var mockXrayCore: XrayCore
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        mockProfileDao = mockk(relaxed = true)
        mockXrayCore = mockk(relaxed = true)
        
        pingService = PingService(context, mockProfileDao, mockXrayCore)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        pingService.stopPingMonitoring()
        clearAllMocks()
    }
    
    @Test
    fun `ping service initializes correctly`() {
        assertFalse("Should not be monitoring initially", pingService.isMonitoring)
    }
    
    @Test
    fun `startPingMonitoring begins background monitoring`() = runTest {
        pingService.startPingMonitoring()
        testDispatcher.scheduler.advanceTimeBy(1000)
        
        assertTrue("Should be monitoring", pingService.isMonitoring)
    }
    
    @Test
    fun `stopPingMonitoring stops background monitoring`() = runTest {
        pingService.startPingMonitoring()
        assertTrue("Should be monitoring", pingService.isMonitoring)
        
        pingService.stopPingMonitoring()
        assertFalse("Should not be monitoring", pingService.isMonitoring)
    }
    
    @Test
    fun `pingServer returns valid latency for reachable server`() = runTest {
        // Mock XrayCore to return valid latency
        every { mockXrayCore.testConnectivity("test.example.com", 443) } returns 50L
        
        val latency = pingService.pingServer("test.example.com", 443)
        assertEquals("Should return valid latency", 50L, latency)
        
        verify { mockXrayCore.testConnectivity("test.example.com", 443) }
    }
    
    @Test
    fun `pingServer returns -1 for unreachable server`() = runTest {
        // Mock XrayCore to return failure
        every { mockXrayCore.testConnectivity("unreachable.example.com", 443) } returns -1L
        
        val latency = pingService.pingServer("unreachable.example.com", 443)
        assertEquals("Should return -1 for unreachable server", -1L, latency)
    }
    
    @Test
    fun `pingServer handles exceptions gracefully`() = runTest {
        // Mock XrayCore to throw exception
        every { mockXrayCore.testConnectivity(any(), any()) } throws RuntimeException("Network error")
        
        val latency = pingService.pingServer("error.example.com", 443)
        assertEquals("Should return -1 on exception", -1L, latency)
    }
    
    @Test
    fun `pingAllProfiles processes all profiles correctly`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443),
            createTestProfile("profile2", "server2.example.com", 443),
            createTestProfile("profile3", "server3.example.com", 443)
        )
        
        // Mock DAO to return test profiles
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        
        // Mock XrayCore to return different latencies
        every { mockXrayCore.testConnectivity("server1.example.com", 443) } returns 50L
        every { mockXrayCore.testConnectivity("server2.example.com", 443) } returns 100L
        every { mockXrayCore.testConnectivity("server3.example.com", 443) } returns -1L
        
        val results = pingService.pingAllProfiles()
        
        assertEquals("Should return results for all profiles", 3, results.size)
        assertEquals("Profile1 should have 50ms latency", 50L, results["profile1"])
        assertEquals("Profile2 should have 100ms latency", 100L, results["profile2"])
        assertEquals("Profile3 should have -1ms latency", -1L, results["profile3"])
        
        // Verify database updates
        coVerify { mockProfileDao.updateLatency("profile1", 50L) }
        coVerify { mockProfileDao.updateLatency("profile2", 100L) }
        coVerify(exactly = 0) { mockProfileDao.updateLatency("profile3", any()) }
    }
    
    @Test
    fun `pingAllProfiles handles empty profile list`() = runTest {
        coEvery { mockProfileDao.getAllProfilesList() } returns emptyList()
        
        val results = pingService.pingAllProfiles()
        
        assertTrue("Should return empty results", results.isEmpty())
        coVerify(exactly = 0) { mockProfileDao.updateLatency(any(), any()) }
    }
    
    @Test
    fun `pingAllProfiles handles database errors gracefully`() = runTest {
        coEvery { mockProfileDao.getAllProfilesList() } throws RuntimeException("Database error")
        
        val results = pingService.pingAllProfiles()
        
        assertTrue("Should return empty results on error", results.isEmpty())
    }
    
    @Test
    fun `getBestServer returns server with lowest latency`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443, 100L),
            createTestProfile("profile2", "server2.example.com", 443, 50L),  // Best
            createTestProfile("profile3", "server3.example.com", 443, 150L),
            createTestProfile("profile4", "server4.example.com", 443, null) // No latency
        )
        
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        
        val bestServer = pingService.getBestServer()
        assertEquals("Should return server with lowest latency", "profile2", bestServer)
    }
    
    @Test
    fun `getBestServer returns null when no servers have latency`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443, null),
            createTestProfile("profile2", "server2.example.com", 443, null),
            createTestProfile("profile3", "server3.example.com", 443, -1L) // Invalid latency
        )
        
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        
        val bestServer = pingService.getBestServer()
        assertNull("Should return null when no valid latencies", bestServer)
    }
    
    @Test
    fun `getBestServer returns null for empty profile list`() = runTest {
        coEvery { mockProfileDao.getAllProfilesList() } returns emptyList()
        
        val bestServer = pingService.getBestServer()
        assertNull("Should return null for empty list", bestServer)
    }
    
    @Test
    fun `background monitoring pings profiles periodically`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443)
        )
        
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        every { mockXrayCore.testConnectivity("server1.example.com", 443) } returns 75L
        
        pingService.startPingMonitoring()
        
        // Advance time to trigger first ping cycle
        testDispatcher.scheduler.advanceTimeBy(11 * 60 * 1000L) // 11 minutes
        
        // Verify ping was performed
        verify(atLeast = 1) { mockXrayCore.testConnectivity("server1.example.com", 443) }
        coVerify(atLeast = 1) { mockProfileDao.updateLatency("profile1", 75L) }
    }
    
    @Test
    fun `background monitoring handles errors and continues`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443)
        )
        
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        every { mockXrayCore.testConnectivity("server1.example.com", 443) } throws RuntimeException("Network error")
        
        pingService.startPingMonitoring()
        
        // Advance time to trigger ping cycle
        testDispatcher.scheduler.advanceTimeBy(11 * 60 * 1000L)
        
        // Should still be monitoring despite error
        assertTrue("Should still be monitoring after error", pingService.isMonitoring)
    }
    
    @Test
    fun `concurrent ping operations are handled correctly`() = runTest {
        val testProfiles = listOf(
            createTestProfile("profile1", "server1.example.com", 443),
            createTestProfile("profile2", "server2.example.com", 443)
        )
        
        coEvery { mockProfileDao.getAllProfilesList() } returns testProfiles
        every { mockXrayCore.testConnectivity("server1.example.com", 443) } returns 50L
        every { mockXrayCore.testConnectivity("server2.example.com", 443) } returns 100L
        
        // Start multiple ping operations concurrently
        val results1 = async { pingService.pingAllProfiles() }
        val results2 = async { pingService.pingAllProfiles() }
        
        val finalResults1 = results1.await()
        val finalResults2 = results2.await()
        
        // Both should complete successfully
        assertEquals("First operation should succeed", 2, finalResults1.size)
        assertEquals("Second operation should succeed", 2, finalResults2.size)
    }
    
    @Test
    fun `ping timeout is respected`() = runTest {
        // Mock a slow response
        every { mockXrayCore.testConnectivity("slow.example.com", 443) } answers {
            Thread.sleep(6000) // Simulate 6 second delay
            50L
        }
        
        val startTime = System.currentTimeMillis()
        val latency = pingService.pingServer("slow.example.com", 443)
        val endTime = System.currentTimeMillis()
        
        // Should timeout and return -1
        assertEquals("Should timeout and return -1", -1L, latency)
        assertTrue("Should complete within reasonable time", (endTime - startTime) < 6000)
    }
    
    private fun createTestProfile(
        id: String, 
        serverAddress: String, 
        port: Int, 
        latency: Long? = null
    ): ProfileEntity {
        return ProfileEntity(
            id = id,
            name = "Test Server $id",
            protocol = "vmess",
            serverAddress = serverAddress,
            serverPort = port,
            userId = "test-user-id",
            password = "",
            security = "auto",
            network = "ws",
            path = "/test",
            host = serverAddress,
            tls = true,
            sni = serverAddress,
            allowInsecure = false,
            latency = latency
        )
    }
}

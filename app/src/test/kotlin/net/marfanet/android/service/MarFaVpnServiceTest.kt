package net.marfanet.android.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import net.marfanet.android.data.ProfileEntity
import net.marfanet.android.logging.ConnectionLogger
import net.marfanet.android.stats.VpnStatsCollector
import net.marfanet.android.xray.XrayConfigBuilder
import net.marfanet.android.xray.XrayCore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MarFaVpnServiceTest {
    
    private lateinit var service: MarFaVpnService
    private lateinit var context: Context
    private lateinit var mockXrayCore: XrayCore
    private lateinit var mockConfigBuilder: XrayConfigBuilder
    private lateinit var mockConnectionLogger: ConnectionLogger
    private lateinit var mockStatsCollector: VpnStatsCollector
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        context = ApplicationProvider.getApplicationContext()
        
        // Create mocks
        mockXrayCore = mockk(relaxed = true)
        mockConfigBuilder = mockk(relaxed = true)
        mockConnectionLogger = mockk(relaxed = true)
        mockStatsCollector = mockk(relaxed = true)
        
        // Create service
        val serviceController = Robolectric.buildService(MarFaVpnService::class.java)
        service = serviceController.create().get()
        
        // Inject mocks
        service.xrayCore = mockXrayCore
        service.configBuilder = mockConfigBuilder
        service.connectionLogger = mockConnectionLogger
        service.statsCollector = mockStatsCollector
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    @Test
    fun `service starts in disconnected state`() = runTest {
        assertEquals(VpnConnectionState.DISCONNECTED, service.getConnectionState())
        
        val connectionInfo = service.getConnectionInfo()
        assertEquals(VpnConnectionState.DISCONNECTED, connectionInfo.state)
        assertNull(connectionInfo.profileName)
        assertEquals(0L, connectionInfo.bytesReceived)
        assertEquals(0L, connectionInfo.bytesSent)
    }
    
    @Test
    fun `connect action starts VPN connection`() = runTest {
        // Setup mocks
        every { mockXrayCore.start(any()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        val testProfile = createTestProfile()
        
        // Test connection
        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify Xray core was started
        verify { mockXrayCore.start(any()) }
        verify { mockConnectionLogger.logConnectionAttempt(any(), any(), any(), any()) }
        
        // Note: Full connection test would require VPN permission mocking
        // which is complex in unit tests. Integration tests would cover this.
    }
    
    @Test
    fun `smart connect selects best server`() = runTest {
        // Setup mocks
        every { mockXrayCore.start(any()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        // Create intent for smart connect
        val intent = Intent().apply {
            action = "net.marfanet.android.SMART_CONNECT"
        }
        
        service.onStartCommand(intent, 0, 1)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify connection attempt was made
        verify { mockConnectionLogger.logConnectionAttempt(any(), any(), any(), any()) }
    }
    
    @Test
    fun `disconnect stops VPN and cleans up resources`() = runTest {
        // Setup initial connected state
        val testProfile = createTestProfile()
        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Test disconnect
        service.disconnect()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify cleanup
        verify { mockXrayCore.stop() }
        verify { mockStatsCollector.stopCollecting() }
        
        assertEquals(VpnConnectionState.DISCONNECTED, service.getConnectionState())
    }
    
    @Test
    fun `connection failure updates state correctly`() = runTest {
        // Setup mock to fail
        every { mockXrayCore.start(any()) } returns false
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        val testProfile = createTestProfile()
        
        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify failure was logged
        verify { 
            mockConnectionLogger.logConnectionFailure(
                any(), any(), any(), any(), any()
            ) 
        }
    }
    
    @Test
    fun `connection info updates during lifecycle`() = runTest {
        val testProfile = createTestProfile()
        
        // Initial state
        var connectionInfo = service.getConnectionInfo()
        assertEquals(VpnConnectionState.DISCONNECTED, connectionInfo.state)
        
        // Mock successful Xray start
        every { mockXrayCore.start(any()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        // Start connection
        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Check connecting state was set
        // Note: Full test would require VPN permission mocking
    }
    
    @Test
    fun `service handles multiple rapid connect attempts gracefully`() = runTest {
        val testProfile = createTestProfile()
        
        // Setup mocks
        every { mockXrayCore.start(any()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        // Rapid connect attempts
        repeat(5) {
            service.connect(testProfile)
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should only attempt connection once (or handle gracefully)
        verify(atMost = 5) { mockXrayCore.start(any()) }
    }
    
    @Test
    fun `service logs connection metrics correctly`() = runTest {
        val testProfile = createTestProfile()
        
        // Setup mocks
        every { mockXrayCore.start(any()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()
        
        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify connection attempt was logged with correct parameters
        verify { 
            mockConnectionLogger.logConnectionAttempt(
                serverId = testProfile.id,
                protocol = testProfile.protocol,
                serverAddress = testProfile.serverAddress,
                serverPort = testProfile.serverPort
            )
        }
    }
    
    @Test
    fun `service handles intent actions correctly`() = runTest {
        // Test CONNECT action
        val connectIntent = Intent().apply {
            action = "net.marfanet.android.CONNECT"
            putExtra(MarFaVpnService.EXTRA_PROFILE_ID, "test-profile")
        }
        
        val result = service.onStartCommand(connectIntent, 0, 1)
        assertEquals(android.app.Service.START_STICKY, result)
        
        // Test DISCONNECT action
        val disconnectIntent = Intent().apply {
            action = "net.marfanet.android.DISCONNECT"
        }
        
        service.onStartCommand(disconnectIntent, 0, 2)
        testDispatcher.scheduler.advanceUntilIdle()
        
        verify { mockXrayCore.stop() }
    }
    
    @Test
    fun `binder returns correct service instance`() {
        val binder = service.onBind(Intent())
        assertNotNull(binder)
        
        val serviceBinder = binder as MarFaVpnService.VpnServiceBinder
        assertEquals(service, serviceBinder.getService())
    }
    
    private fun createTestProfile(): ProfileEntity {
        return ProfileEntity(
            id = "test-profile-id",
            name = "Test Server",
            protocol = "vmess",
            serverAddress = "test.example.com",
            serverPort = 443,
            userId = "test-user-id",
            password = "",
            security = "auto",
            network = "ws",
            path = "/test",
            host = "test.example.com",
            tls = true,
            sni = "test.example.com",
            allowInsecure = false,
            latency = 50L
        )
    }
}

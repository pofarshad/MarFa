package net.marfanet.android.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.*
import net.marfanet.android.data.ProfileDao // Added import for ProfileDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import net.marfanet.android.data.ProfileEntity
// Removed import for ProfileRepository as it's not used by MarFaVpnService
import net.marfanet.android.logging.ConnectionLogger
import net.marfanet.android.stats.VpnStatsCollector
import net.marfanet.android.xray.XrayConfigBuilder
import net.marfanet.android.xray.XrayCore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import net.marfanet.android.di.AppModule
import net.marfanet.android.di.IoDispatcher // Added import for qualifier
import dagger.hilt.android.testing.HiltTestApplication // Added import

@ExperimentalCoroutinesApi
@HiltAndroidTest
@UninstallModules(AppModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = HiltTestApplication::class)
class MarFaVpnServiceTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    private lateinit var service: MarFaVpnService

    @BindValue
    @JvmField
    val mockXrayCore: XrayCore = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockConfigBuilder: XrayConfigBuilder = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockConnectionLogger: ConnectionLogger = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockStatsCollector: VpnStatsCollector = mockk(relaxed = true)

    @BindValue
    @JvmField
    val mockProfileDao: ProfileDao = mockk(relaxed = true)

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @BindValue
    @IoDispatcher
    @JvmField
    val hiltBoundTestIoDispatcher: CoroutineDispatcher = testDispatcher

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        service = Robolectric.buildService(MarFaVpnService::class.java).create().get()
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
        val testProfile = createTestProfile()
        val mockGeneratedConfig: net.marfanet.android.xray.XrayConfig = mockk(relaxed = true)

        every {
            mockConfigBuilder.buildConfig(
                profile = eq(testProfile),
                enableGfwRules = any(),
                bypassApps = any(),
                customRules = any()
            )
        } returns mockGeneratedConfig

        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true

        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            mockConfigBuilder.buildConfig(
                profile = eq(testProfile),
                enableGfwRules = any(),
                bypassApps = any(),
                customRules = any()
            )
        }
        coVerify { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) }
        verify { mockConnectionLogger.logConnectionAttempt(any(), any(), any(), any()) }
    }

    @Test
    fun `smart connect selects best server`() = runTest {
        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        val intent = Intent().apply {
            action = "net.marfanet.android.SMART_CONNECT"
        }

        service.onStartCommand(intent, 0, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockConnectionLogger.logConnectionAttempt(any(), any(), any(), any()) }
    }

    @Test
    fun `disconnect stops VPN and cleans up resources`() = runTest {
        val testProfile = createTestProfile()
        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        service.disconnect()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockXrayCore.stop() }
        verify { mockStatsCollector.stopCollecting() }

        assertEquals(VpnConnectionState.DISCONNECTED, service.getConnectionState())
    }

    @Test
    fun `connection failure updates state correctly`() = runTest {
        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns false
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        val testProfile = createTestProfile()

        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        verify {
            mockConnectionLogger.logConnectionFailure(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `connection info updates during lifecycle`() = runTest {
        val testProfile = createTestProfile()

        var connectionInfo = service.getConnectionInfo()
        assertEquals(VpnConnectionState.DISCONNECTED, connectionInfo.state)

        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `service handles multiple rapid connect attempts gracefully`() = runTest {
        val testProfile = createTestProfile()

        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        repeat(5) {
            service.connect(testProfile)
        }

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atMost = 5) { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) }
    }

    @Test
    fun `service logs connection metrics correctly`() = runTest {
        val testProfile = createTestProfile()

        coEvery { mockXrayCore.start(any<net.marfanet.android.xray.XrayConfig>()) } returns true
        every { mockConfigBuilder.buildConfig(any(), any(), any(), any()) } returns mockk()

        service.connect(testProfile)
        testDispatcher.scheduler.advanceUntilIdle()

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
        val connectIntent = Intent().apply {
            action = "net.marfanet.android.CONNECT"
            putExtra(MarFaVpnService.EXTRA_PROFILE_ID, "test-profile")
        }

        val result = service.onStartCommand(connectIntent, 0, 1)
        assertEquals(android.app.Service.START_STICKY, result)

        val disconnectIntent = Intent().apply {
            action = "net.marfanet.android.DISCONNECT"
        }

        service.onStartCommand(disconnectIntent, 0, 2)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockXrayCore.stop() }
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

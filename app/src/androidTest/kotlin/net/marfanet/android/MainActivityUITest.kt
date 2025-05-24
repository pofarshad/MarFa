package net.marfanet.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import net.marfanet.android.service.ConnectionInfo
import net.marfanet.android.service.VpnConnectionState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityUITest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun mainScreen_displaysCorrectInitialState() {
        // Verify app title is displayed
        composeTestRule.onNodeWithText("MarFaNet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Xray-Powered VPN").assertIsDisplayed()
        
        // Verify initial disconnected state
        composeTestRule.onNodeWithText("Disconnected").assertIsDisplayed()
        
        // Verify connect buttons are displayed
        composeTestRule.onNodeWithText("Smart").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
        
        // Verify settings icon is displayed
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
    
    @Test
    fun connectButton_isClickableWhenDisconnected() {
        // Find and click the connect button
        composeTestRule.onNodeWithText("Connect").assertIsEnabled()
        composeTestRule.onNodeWithText("Connect").performClick()
        
        // Note: In a real test, we would verify the VPN permission dialog appears
        // or mock the VPN service to verify connection attempt
    }
    
    @Test
    fun smartConnectButton_isClickableWhenDisconnected() {
        // Find and click the smart connect button
        composeTestRule.onNodeWithText("Smart").assertIsEnabled()
        composeTestRule.onNodeWithText("Smart").performClick()
        
        // Note: In a real test, we would verify smart connect logic is triggered
    }
    
    @Test
    fun connectionCircle_isClickableWhenDisconnected() {
        // Find the main connection circle (VPN lock icon should be visible)
        composeTestRule.onNodeWithContentDescription("VPN lock").assertIsDisplayed()
        
        // The circle should be clickable
        composeTestRule.onNode(hasClickAction()).assertExists()
    }
    
    @Test
    fun quickActionCards_areDisplayedAndClickable() {
        // Verify quick action cards are displayed
        composeTestRule.onNodeWithText("Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Split Tunnel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rules").assertIsDisplayed()
        
        // Verify they are clickable
        composeTestRule.onNodeWithText("Profiles").assertHasClickAction()
        composeTestRule.onNodeWithText("Split Tunnel").assertHasClickAction()
        composeTestRule.onNodeWithText("Statistics").assertHasClickAction()
        composeTestRule.onNodeWithText("Rules").assertHasClickAction()
    }
    
    @Test
    fun footer_displaysCorrectVersion() {
        // Verify footer with version info
        composeTestRule.onNodeWithText("v1.1.0-alpha1 • Built for Iran 🇮🇷").assertIsDisplayed()
    }
    
    @Test
    fun settingsButton_isClickable() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    // Note: The following tests would require mocking the VPN service state
    // In a real implementation, you would inject mock services to test different states
    
    /*
    @Test
    fun connectedState_displaysCorrectUI() {
        // This would require mocking the connection state to CONNECTED
        // and verifying the UI updates accordingly:
        // - "Protected" status text
        // - Green connection circle
        // - "Disconnect" button
        // - Statistics cards visible
        // - Profile name and latency displayed
    }
    
    @Test
    fun connectingState_showsProgressIndicator() {
        // This would require mocking the connection state to CONNECTING
        // and verifying:
        // - "Connecting..." status text
        // - Animated progress indicator in circle
        // - Buttons disabled during connection
        // - Pulsing animation on connection circle
    }
    
    @Test
    fun errorState_displaysErrorMessage() {
        // This would require mocking the connection state to ERROR
        // and verifying:
        // - "Connection Failed" status text
        // - Red color scheme
        // - Error icon in connection circle
        // - Connect button re-enabled for retry
    }
    
    @Test
    fun statsCards_displayCorrectData() {
        // This would require mocking a connected state with traffic data
        // and verifying:
        // - Download/Upload cards are visible
        // - Correct byte formatting (B, KB, MB, GB)
        // - Real-time updates of statistics
    }
    */
}

/**
 * Compose UI Test for MarFaNetMainScreen composable in isolation
 */
@RunWith(AndroidJUnit4::class)
class MarFaNetMainScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun mainScreen_disconnectedState_displaysCorrectly() {
        val disconnectedInfo = ConnectionInfo(VpnConnectionState.DISCONNECTED)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = disconnectedInfo,
                    onConnectClick = {},
                    onSmartConnectClick = {},
                    onDisconnectClick = {}
                )
            }
        }
        
        // Verify disconnected state UI
        composeTestRule.onNodeWithText("Disconnected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart").assertIsDisplayed()
        
        // Verify buttons are enabled
        composeTestRule.onNodeWithText("Connect").assertIsEnabled()
        composeTestRule.onNodeWithText("Smart").assertIsEnabled()
    }
    
    @Test
    fun mainScreen_connectedState_displaysCorrectly() {
        val connectedInfo = ConnectionInfo(
            state = VpnConnectionState.CONNECTED,
            profileName = "Test Server",
            protocol = "vmess",
            latency = 50L,
            bytesReceived = 1024 * 1024, // 1 MB
            bytesSent = 512 * 1024 // 512 KB
        )
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = connectedInfo,
                    onConnectClick = {},
                    onSmartConnectClick = {},
                    onDisconnectClick = {}
                )
            }
        }
        
        // Verify connected state UI
        composeTestRule.onNodeWithText("Protected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Server").assertIsDisplayed()
        composeTestRule.onNodeWithText("50ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("VMESS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect").assertIsDisplayed()
        
        // Verify stats cards
        composeTestRule.onNodeWithText("1MB").assertIsDisplayed() // Download
        composeTestRule.onNodeWithText("512KB").assertIsDisplayed() // Upload
        
        // Verify smart connect is disabled when connected
        composeTestRule.onNodeWithText("Smart").assertIsNotEnabled()
    }
    
    @Test
    fun mainScreen_connectingState_displaysCorrectly() {
        val connectingInfo = ConnectionInfo(VpnConnectionState.CONNECTING)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = connectingInfo,
                    onConnectClick = {},
                    onSmartConnectClick = {},
                    onDisconnectClick = {}
                )
            }
        }
        
        // Verify connecting state UI
        composeTestRule.onNodeWithText("Connecting...").assertIsDisplayed()
        
        // Verify buttons are disabled during connection
        composeTestRule.onNodeWithText("Connect").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Smart").assertIsNotEnabled()
    }
    
    @Test
    fun mainScreen_errorState_displaysCorrectly() {
        val errorInfo = ConnectionInfo(VpnConnectionState.ERROR)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = errorInfo,
                    onConnectClick = {},
                    onSmartConnectClick = {},
                    onDisconnectClick = {}
                )
            }
        }
        
        // Verify error state UI
        composeTestRule.onNodeWithText("Connection Failed").assertIsDisplayed()
        
        // Verify buttons are re-enabled for retry
        composeTestRule.onNodeWithText("Connect").assertIsEnabled()
        composeTestRule.onNodeWithText("Smart").assertIsEnabled()
    }
    
    @Test
    fun clickCallbacks_areTriggeredCorrectly() {
        var connectClicked = false
        var smartConnectClicked = false
        var disconnectClicked = false
        
        val disconnectedInfo = ConnectionInfo(VpnConnectionState.DISCONNECTED)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = disconnectedInfo,
                    onConnectClick = { connectClicked = true },
                    onSmartConnectClick = { smartConnectClicked = true },
                    onDisconnectClick = { disconnectClicked = true }
                )
            }
        }
        
        // Test connect button click
        composeTestRule.onNodeWithText("Connect").performClick()
        assert(connectClicked) { "Connect callback should be triggered" }
        
        // Test smart connect button click
        composeTestRule.onNodeWithText("Smart").performClick()
        assert(smartConnectClicked) { "Smart connect callback should be triggered" }
    }
    
    @Test
    fun connectionCircle_clickTriggersSmartConnect() {
        var smartConnectClicked = false
        val disconnectedInfo = ConnectionInfo(VpnConnectionState.DISCONNECTED)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = disconnectedInfo,
                    onConnectClick = {},
                    onSmartConnectClick = { smartConnectClicked = true },
                    onDisconnectClick = {}
                )
            }
        }
        
        // Click on the main connection circle
        composeTestRule.onNode(hasClickAction()).onFirst().performClick()
        assert(smartConnectClicked) { "Smart connect should be triggered by circle click" }
    }
    
    @Test
    fun byteFormatting_displaysCorrectly() {
        val testCases = listOf(
            512L to "512B",
            1024L to "1KB",
            1024 * 1024L to "1MB",
            1024 * 1024 * 1024L to "1GB"
        )
        
        testCases.forEach { (bytes, expected) ->
            val connectedInfo = ConnectionInfo(
                state = VpnConnectionState.CONNECTED,
                profileName = "Test Server",
                bytesReceived = bytes,
                bytesSent = 0L
            )
            
            composeTestRule.setContent {
                MarFaNetTheme {
                    MarFaNetMainScreen(
                        connectionInfo = connectedInfo,
                        onConnectClick = {},
                        onSmartConnectClick = {},
                        onDisconnectClick = {}
                    )
                }
            }
            
            composeTestRule.onNodeWithText(expected).assertIsDisplayed()
        }
    }
    
    @Test
    fun quickActions_performClickCorrectly() {
        val disconnectedInfo = ConnectionInfo(VpnConnectionState.DISCONNECTED)
        
        composeTestRule.setContent {
            MarFaNetTheme {
                MarFaNetMainScreen(
                    connectionInfo = disconnectedInfo,
                    onConnectClick = {},
                    onSmartConnectClick = {},
                    onDisconnectClick = {}
                )
            }
        }
        
        // Test that quick action cards can be clicked
        composeTestRule.onNodeWithText("Profiles").performClick()
        composeTestRule.onNodeWithText("Split Tunnel").performClick()
        composeTestRule.onNodeWithText("Statistics").performClick()
        composeTestRule.onNodeWithText("Rules").performClick()
        
        // No assertions here as the clicks don't have callbacks in the test
        // In a real app, these would navigate to different screens
    }
}

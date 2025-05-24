package net.marfanet.android.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.marfanet.android.logging.ConnectionLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages VPN connection state and network transitions
 * Implements automatic reconnection and network switching
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val context: Context,
    private val logger: ConnectionLogger
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private var lastKnownNetwork: Network? = null
    private var isReconnecting = false
    
    init {
        registerNetworkCallback()
    }
    
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
            
        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkChange(network)
            }
            
            override fun onLost(network: Network) {
                if (network == lastKnownNetwork) {
                    handleNetworkLost()
                }
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                handleCapabilitiesChange(network, capabilities)
            }
        })
    }
    
    private fun handleNetworkChange(network: Network) {
        scope.launch {
            if (lastKnownNetwork != network && _connectionState.value is ConnectionState.Connected) {
                logger.logConnectionAttempt(
                    serverId = "auto_reconnect",
                    protocol = "auto",
                    serverAddress = "switching_network",
                    serverPort = 0
                )
                
                isReconnecting = true
                _connectionState.value = ConnectionState.Connecting
                
                // Attempt reconnection with exponential backoff
                var attempt = 0
                var delay = 1000L
                
                while (isReconnecting && attempt < 3) {
                    try {
                        // Attempt to reconnect using last known good configuration
                        reconnectVpn()
                        isReconnecting = false
                        _connectionState.value = ConnectionState.Connected
                        logger.logConnectionSuccess(
                            serverId = "auto_reconnect",
                            protocol = "auto",
                            connectionTimeMs = delay,
                            selectedServer = "auto"
                        )
                    } catch (e: Exception) {
                        attempt++
                        if (attempt < 3) {
                            kotlinx.coroutines.delay(delay)
                            delay *= 2 // Exponential backoff
                        } else {
                            _connectionState.value = ConnectionState.Error(e)
                            logger.logConnectionFailure(
                                serverId = "auto_reconnect",
                                protocol = "auto",
                                errorCode = "reconnect_failed",
                                errorMessage = e.message ?: "Unknown error",
                                retryAttempt = attempt
                            )
                        }
                    }
                }
            }
            lastKnownNetwork = network
        }
    }
    
    private fun handleNetworkLost() {
        scope.launch {
            _connectionState.value = ConnectionState.Disconnected
            logger.logDisconnection(
                serverId = "auto_reconnect",
                protocol = "auto",
                sessionDurationMs = 0,
                disconnectReason = "network_lost",
                networkChange = true
            )
        }
    }
    
    private fun handleCapabilitiesChange(network: Network, capabilities: NetworkCapabilities) {
        // Update connection metrics based on new capabilities
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        
        // Log network type changes
        logger.logConnectionAttempt(
            serverId = "network_change",
            protocol = when {
                hasWifi -> "wifi"
                hasCellular -> "cellular"
                else -> "other"
            },
            serverAddress = "capability_change",
            serverPort = 0
        )
    }
    
    private suspend fun reconnectVpn() {
        // Implementation would integrate with your VPN service
        // This is a placeholder for the actual reconnection logic
    }
    
    fun cleanup() {
        isReconnecting = false
        scope.launch {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Error(val exception: Exception) : ConnectionState()
}

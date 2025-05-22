package net.marfanet.android.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for VPN Engine Selection
 * WG-003: Manages engine switching between Xray and WireGuard
 */
class VpnEngineViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(VpnEngineUiState())
    val uiState: StateFlow<VpnEngineUiState> = _uiState.asStateFlow()
    
    init {
        checkWireGuardAvailability()
    }
    
    fun selectEngine(engine: VpnEngine) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedEngine = engine)
            // Save preference and notify VPN service
            saveEnginePreference(engine)
        }
    }
    
    private fun checkWireGuardAvailability() {
        viewModelScope.launch {
            val isAvailable = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
            _uiState.value = _uiState.value.copy(isWireGuardAvailable = isAvailable)
        }
    }
    
    private suspend fun saveEnginePreference(engine: VpnEngine) {
        // Implementation for saving preference
        // This would integrate with SharedPreferences or Room
    }
}

/**
 * VPN Engine types
 */
enum class VpnEngine {
    XRAY,
    WIREGUARD
}

/**
 * UI state for VPN Engine settings
 */
data class VpnEngineUiState(
    val selectedEngine: VpnEngine = VpnEngine.XRAY,
    val isWireGuardAvailable: Boolean = false
)
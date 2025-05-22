package net.marfanet.android.ui.appselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.marfanet.android.data.AppRule

/**
 * ViewModel for Split-Tunneling App Selector
 * STU-003: Manages app selection state and Room database operations
 */
class AppSelectorViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(AppSelectorUiState())
    val uiState: StateFlow<AppSelectorUiState> = _uiState.asStateFlow()
    
    init {
        loadInstalledApps()
    }
    
    fun updateSearchQuery(query: String) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredApps = filterApps(currentState.allApps, query)
        )
    }
    
    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedApps = currentState.allApps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(isEnabled = !app.isEnabled)
                } else {
                    app
                }
            }
            
            _uiState.value = currentState.copy(
                allApps = updatedApps,
                filteredApps = filterApps(updatedApps, currentState.searchQuery),
                enabledCount = updatedApps.count { it.isEnabled }
            )
        }
    }
    
    fun updateRuleType(packageName: String, ruleType: AppRule.RuleType) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val updatedApps = currentState.allApps.map { app ->
                if (app.packageName == packageName) {
                    app.copy(ruleType = ruleType)
                } else {
                    app
                }
            }
            
            _uiState.value = currentState.copy(
                allApps = updatedApps,
                filteredApps = filterApps(updatedApps, currentState.searchQuery)
            )
        }
    }
    
    private fun loadInstalledApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Sample apps for demonstration
                val sampleApps = listOf(
                    AppRule("com.android.chrome", "Chrome Browser", false),
                    AppRule("com.whatsapp", "WhatsApp", false),
                    AppRule("com.instagram.android", "Instagram", false),
                    AppRule("com.telegram.messenger", "Telegram", false),
                    AppRule("com.spotify.music", "Spotify", false),
                    AppRule("com.netflix.mediaclient", "Netflix", false),
                    AppRule("com.twitter.android", "Twitter", false),
                    AppRule("com.facebook.katana", "Facebook", false)
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allApps = sampleApps,
                    filteredApps = sampleApps,
                    totalCount = sampleApps.size,
                    enabledCount = 0
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun filterApps(apps: List<AppRule>, query: String): List<AppRule> {
        if (query.isBlank()) return apps
        return apps.filter { app ->
            app.appName.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
        }
    }
}

/**
 * UI state for App Selector screen
 */
data class AppSelectorUiState(
    val isLoading: Boolean = false,
    val allApps: List<AppRule> = emptyList(),
    val filteredApps: List<AppRule> = emptyList(),
    val searchQuery: String = "",
    val enabledCount: Int = 0,
    val totalCount: Int = 0,
    val error: String? = null
)
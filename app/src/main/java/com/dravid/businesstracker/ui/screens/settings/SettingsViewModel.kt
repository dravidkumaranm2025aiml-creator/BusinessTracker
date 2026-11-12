package com.dravid.businesstracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dravid.businesstracker.data.local.BusinessDatabase
import com.dravid.businesstracker.data.repository.ThemePreferencesRepository
import com.dravid.businesstracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val appName: String = "BusinessTracker",
    val appVersion: String = "1.0.0",
    val buildType: String = "Production Ready (Offline-First)",
    val storageArchitecture: String = "Local SQLite via Room 2.8",
    val databaseVersion: Int = BusinessDatabase.DATABASE_VERSION,
)

class SettingsViewModel(
    private val themeRepository: ThemePreferencesRepository,
    private val databaseVersion: Int = BusinessDatabase.DATABASE_VERSION,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = themeRepository.themeMode
        .map { mode ->
            SettingsUiState(
                selectedThemeMode = mode,
                databaseVersion = databaseVersion,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = SettingsUiState(
                selectedThemeMode = themeRepository.themeMode.value,
                databaseVersion = databaseVersion,
            ),
        )

    fun onThemeSelected(mode: AppThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }
}

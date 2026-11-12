package com.dravid.businesstracker.data.repository

import com.dravid.businesstracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface managing user theme preferences with reactive state updates.
 */
interface ThemePreferencesRepository {
    /**
     * Observable state flow of the currently configured application theme mode.
     */
    val themeMode: StateFlow<AppThemeMode>

    /**
     * Persists and updates the application theme mode.
     */
    suspend fun setThemeMode(mode: AppThemeMode)
}

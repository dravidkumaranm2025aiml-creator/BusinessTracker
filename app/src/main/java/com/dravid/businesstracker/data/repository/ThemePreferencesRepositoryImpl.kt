package com.dravid.businesstracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.dravid.businesstracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SharedPreferences-backed implementation of [ThemePreferencesRepository].
 * Ensures synchronous immediate read on app launch to eliminate screen flicker.
 */
class ThemePreferencesRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
) : ThemePreferencesRepository {

    constructor(context: Context, prefsName: String = PREFS_NAME) : this(
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE),
    )

    private val _themeMode: MutableStateFlow<AppThemeMode>

    init {
        val storedName = sharedPreferences.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        val initialMode = try {
            if (storedName != null) AppThemeMode.valueOf(storedName) else AppThemeMode.SYSTEM
        } catch (_: IllegalArgumentException) {
            AppThemeMode.SYSTEM
        }
        _themeMode = MutableStateFlow(initialMode)
    }

    override val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: AppThemeMode) {
        sharedPreferences.edit {
            putString(KEY_THEME_MODE, mode.name)
        }
        _themeMode.value = mode
    }

    companion object {
        const val PREFS_NAME = "business_tracker_preferences"
        const val KEY_THEME_MODE = "key_app_theme_mode"
    }
}

package com.dravid.businesstracker.ui.screens.settings

import com.dravid.businesstracker.data.repository.ThemePreferencesRepository
import com.dravid.businesstracker.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeThemeRepo: FakeThemePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeThemeRepo = FakeThemePreferencesRepository()
        viewModel = SettingsViewModel(fakeThemeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValuesAndReflectsThemeRepository() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals(AppThemeMode.SYSTEM, state.selectedThemeMode)
        assertEquals("BusinessTracker", state.appName)
        assertEquals("1.0.0", state.appVersion)
        assertEquals(com.dravid.businesstracker.data.local.BusinessDatabase.DATABASE_VERSION, state.databaseVersion)
        assertEquals(4, state.databaseVersion)
    }

    @Test
    fun onThemeSelected_updatesThemeRepositoryAndUiState() = runTest {
        viewModel.onThemeSelected(AppThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, fakeThemeRepo.themeMode.first())
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.selectedThemeMode)

        viewModel.onThemeSelected(AppThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(AppThemeMode.LIGHT, fakeThemeRepo.themeMode.first())
        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.selectedThemeMode)
    }

    private class FakeThemePreferencesRepository(
        initialMode: AppThemeMode = AppThemeMode.SYSTEM,
    ) : ThemePreferencesRepository {
        private val _mode = MutableStateFlow(initialMode)
        override val themeMode: StateFlow<AppThemeMode> = _mode.asStateFlow()

        override suspend fun setThemeMode(mode: AppThemeMode) {
            _mode.value = mode
        }
    }
}

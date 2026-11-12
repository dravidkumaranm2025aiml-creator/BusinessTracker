package com.dravid.businesstracker.data.repository

import android.content.SharedPreferences
import com.dravid.businesstracker.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class ThemePreferencesRepositoryTest {

    private val storage = mutableMapOf<String, Any?>()
    private lateinit var sharedPrefsProxy: SharedPreferences
    private lateinit var repository: ThemePreferencesRepository

    @Before
    fun setup() {
        storage.clear()
        val editorProxy = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java),
        ) { _, method, args ->
            when (method.name) {
                "putString" -> {
                    storage[args[0] as String] = args[1]
                    null
                }
                "apply", "commit" -> null
                else -> null
            }
        } as SharedPreferences.Editor

        sharedPrefsProxy = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getString" -> {
                    val key = args[0] as String
                    val defVal = args[1] as? String
                    storage[key] as? String ?: defVal
                }
                "edit" -> editorProxy
                else -> null
            }
        } as SharedPreferences

        repository = ThemePreferencesRepositoryImpl(sharedPrefsProxy)
    }

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        assertEquals(AppThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun setThemeMode_updatesStateAndPersists() = runTest {
        repository.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, repository.themeMode.first())
        assertEquals("DARK", storage[ThemePreferencesRepositoryImpl.KEY_THEME_MODE])

        repository.setThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, repository.themeMode.first())
        assertEquals("LIGHT", storage[ThemePreferencesRepositoryImpl.KEY_THEME_MODE])

        repository.setThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals("SYSTEM", storage[ThemePreferencesRepositoryImpl.KEY_THEME_MODE])
    }

    @Test
    fun initialMode_readFromExistingStorage() = runTest {
        storage[ThemePreferencesRepositoryImpl.KEY_THEME_MODE] = "DARK"
        val restoredRepo = ThemePreferencesRepositoryImpl(sharedPrefsProxy)
        assertEquals(AppThemeMode.DARK, restoredRepo.themeMode.first())
    }
}

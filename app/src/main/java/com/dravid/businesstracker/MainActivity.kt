package com.dravid.businesstracker

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import com.dravid.businesstracker.ui.screens.main.MainScreen
import com.dravid.businesstracker.ui.theme.AppThemeMode
import com.dravid.businesstracker.ui.theme.BusinessTrackerTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as? BusinessTrackerApplication
        val themeFlow = app?.themeRepository?.themeMode ?: MutableStateFlow(AppThemeMode.SYSTEM)

        setContent {
            val currentThemeMode by themeFlow.collectAsState()
            BusinessTrackerTheme(themeMode = currentThemeMode) {
                MainScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    BusinessTrackerTheme {
        MainScreen()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenDarkPreview() {
    BusinessTrackerTheme(darkTheme = true) {
        MainScreen()
    }
}

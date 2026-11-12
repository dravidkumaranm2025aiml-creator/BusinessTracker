package com.dravid.businesstracker.ui.theme

/**
 * User selectable application theme mode.
 */
enum class AppThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follows device light/dark appearance"),
    LIGHT("Light", "Clean crisp white & professional blue theme"),
    DARK("Dark", "Midnight navy & ice blue contrast theme")
}

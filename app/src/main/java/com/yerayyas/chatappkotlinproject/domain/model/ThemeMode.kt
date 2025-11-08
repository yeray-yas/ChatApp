package com.yerayyas.chatappkotlinproject.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true
)
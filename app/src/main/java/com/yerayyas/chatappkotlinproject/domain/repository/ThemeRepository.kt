package com.yerayyas.chatappkotlinproject.domain.repository

import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themePreferencesFlow: Flow<ThemePreferences>
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateDynamicColors(useDynamicColors: Boolean)
}
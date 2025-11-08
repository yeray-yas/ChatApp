package com.yerayyas.chatappkotlinproject.data.repository

import com.yerayyas.chatappkotlinproject.data.datasource.ThemeDataSource
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themeDataSource: ThemeDataSource
) : ThemeRepository {

    override val themePreferencesFlow: Flow<ThemePreferences> =
        themeDataSource.themePreferencesFlow

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        themeDataSource.updateThemeMode(themeMode)
    }

    override suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        themeDataSource.updateDynamicColors(useDynamicColors)
    }
}
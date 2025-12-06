package com.yerayyas.chatappkotlinproject.data.repository

import com.yerayyas.chatappkotlinproject.data.datasource.ThemeDataSource
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [ThemeRepository] within the Data Layer.
 *
 * This repository acts as the single point of entry for theme operations, delegating
 * actual persistence logic to the [ThemeDataSource]. It ensures that the Domain Layer
 * remains agnostic of the underlying storage mechanism (DataStore).
 *
 * **Key Responsibilities:**
 * - Bridging Domain and Data layers.
 * - Exposing [ThemePreferences] as a reactive stream.
 * - Delegating write operations to the persistent storage.
 *
 * @property themeDataSource The local data source handling DataStore operations.
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themeDataSource: ThemeDataSource
) : ThemeRepository {

    /**
     * Exposes the current theme configuration as a hot stream.
     *
     * Delegates to [ThemeDataSource.themePreferencesFlow] to provide real-time
     * updates backed by the underlying persistence layer.
     *
     * @return A [Flow] emitting [ThemePreferences] whenever settings change.
     */
    override val themePreferencesFlow: Flow<ThemePreferences> =
        themeDataSource.themePreferencesFlow

    /**
     * Persists the user's chosen theme mode.
     *
     * This change triggers a new emission in [themePreferencesFlow].
     *
     * @param themeMode The new [ThemeMode] to be saved.
     */
    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        themeDataSource.updateThemeMode(themeMode)
    }

    /**
     * Persists the preference for dynamic colors (Material You).
     *
     * This change triggers a new emission in [themePreferencesFlow].
     *
     * @param useDynamicColors `true` to enable dynamic theming.
     */
    override suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        themeDataSource.updateDynamicColors(useDynamicColors)
    }
}

package com.yerayyas.chatappkotlinproject.data.repository

import com.yerayyas.chatappkotlinproject.data.datasource.ThemeDataSource
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [ThemeRepository] for managing application theme preferences.
 *
 * This repository serves as a bridge between the domain layer and the data source layer,
 * orchestrating theme preference operations through the ThemeDataSource. It follows
 * the Repository pattern to provide a clean abstraction over data persistence operations.
 *
 * Key responsibilities:
 * - Exposing reactive theme preference streams from the data source
 * - Delegating theme mode updates to the underlying data source
 * - Managing dynamic color preference changes
 * - Maintaining consistency between domain and data layer contracts
 *
 * The repository provides a reactive interface through Flow, allowing the UI layer
 * to observe theme changes in real-time and update accordingly. All preference
 * changes are persisted using Android's DataStore for type-safe storage.
 *
 * Architecture pattern: Repository Pattern
 * - Implements the domain repository interface
 * - Delegates to data source for actual persistence operations
 * - Provides clean separation between domain and data layers
 * - Maintains thread-safe operations through underlying DataStore
 *
 * @property themeDataSource The data source responsible for theme preference persistence
 */
@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val themeDataSource: ThemeDataSource
) : ThemeRepository {

    /**
     * Reactive stream of theme preferences.
     *
     * This Flow emits the current theme preferences whenever they change,
     * allowing the UI to react immediately to user preference updates.
     * The stream is backed by DataStore for efficient, type-safe persistence.
     *
     * @return Flow of current [ThemePreferences] including theme mode and dynamic color settings
     */
    override val themePreferencesFlow: Flow<ThemePreferences> =
        themeDataSource.themePreferencesFlow

    /**
     * Updates the application theme mode preference.
     *
     * This operation persists the new theme mode setting and triggers
     * an update in the [themePreferencesFlow] for reactive UI updates.
     *
     * @param themeMode The new theme mode to apply (Light, Dark, or System)
     */
    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        themeDataSource.updateThemeMode(themeMode)
    }

    /**
     * Updates the dynamic colors preference for Material You theming.
     *
     * This operation persists the dynamic color setting and triggers
     * an update in the [themePreferencesFlow] for reactive UI updates.
     *
     * @param useDynamicColors Whether to enable Material You dynamic theming
     */
    override suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        themeDataSource.updateDynamicColors(useDynamicColors)
    }
}
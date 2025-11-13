package com.yerayyas.chatappkotlinproject.data.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Data source for managing theme preferences using DataStore.
 *
 * This class handles the persistence and retrieval of user theme preferences
 * using Android's DataStore for type-safe, asynchronous data storage.
 * It provides reactive access to theme settings through Flow emissions.
 *
 * Key features:
 * - Type-safe preference storage using DataStore
 * - Reactive theme updates through Flow
 * - Support for theme mode selection (Light, Dark, System)
 * - Dynamic color configuration management
 * - Thread-safe operations with coroutine support
 * - Default value handling for first-time users
 *
 * Architecture pattern: Data Layer - DataSource
 * - Abstracts data storage implementation details
 * - Provides clean interface for theme preference operations
 * - Handles migration and default value scenarios
 * - Ensures data consistency and type safety
 */
@Singleton
class ThemeDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Preference keys for theme-related settings.
     * Centralized key management ensures consistency and prevents key conflicts.
     */
    private object Keys {
        /** Key for storing the selected theme mode (Light/Dark/System) */
        val THEME_MODE = stringPreferencesKey("theme_mode")

        /** Key for storing dynamic color preference (Material You support) */
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    /**
     * Reactive flow of theme preferences.
     *
     * This Flow emits [ThemePreferences] objects whenever theme settings change,
     * allowing the UI to react immediately to preference updates. The flow provides
     * default values for first-time users and handles data migration scenarios.
     *
     * Default values:
     * - Theme Mode: SYSTEM (follows device theme)
     * - Dynamic Colors: true (enables Material You theming)
     *
     * @return Flow emitting current theme preferences
     */
    val themePreferencesFlow: Flow<ThemePreferences> = context.dataStore.data.map { preferences ->
        ThemePreferences(
            themeMode = ThemeMode.valueOf(
                preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ),
            useDynamicColors = preferences[Keys.USE_DYNAMIC_COLORS] ?: true
        )
    }

    /**
     * Updates the theme mode preference.
     *
     * This method persists the user's theme mode selection (Light, Dark, or System)
     * to DataStore. The change will be automatically reflected in [themePreferencesFlow].
     *
     * @param themeMode The new theme mode to apply
     * @throws Exception if the update operation fails
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    /**
     * Updates the dynamic colors preference.
     *
     * This method persists the user's dynamic color preference (Material You theming)
     * to DataStore. The change will be automatically reflected in [themePreferencesFlow].
     *
     * Dynamic colors (Material You):
     * - true: Uses device wallpaper colors for theming
     * - false: Uses app's predefined color scheme
     *
     * @param useDynamicColors Whether to enable dynamic color theming
     * @throws Exception if the update operation fails
     */
    suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_DYNAMIC_COLORS] = useDynamicColors
        }
    }
}
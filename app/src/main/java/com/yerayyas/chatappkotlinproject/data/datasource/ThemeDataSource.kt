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
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Ensures a single instance of DataStore per process
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Data source responsible for persisting and retrieving application theme settings.
 *
 * Implements the Data Layer contract for theme management using Android's [DataStore].
 * It provides type-safe, asynchronous storage and reactive updates via [Flow].
 *
 * **Key Features:**
 * - **Type Integration:** Maps raw preferences to domain model [ThemePreferences].
 * - **Fault Tolerance:** Gracefully handles invalid or obsolete enum values.
 * - **Reactive:** Emits new values immediately upon storage updates.
 *
 * **Architecture:**
 * Acts as the Single Source of Truth (SSOT) for theme configuration, abstracting
 * the underlying storage mechanism from the domain layer.
 */
@Singleton
class ThemeDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    /**
     * A hot stream of the current [ThemePreferences].
     *
     * This Flow emits the current theme configuration immediately upon subscription.
     *
     * **Robustness:**
     * Uses [runCatching] to safely parse the [ThemeMode]. If the stored value matches
     * no known enum constant (e.g., after a refactor), it falls back to [ThemeMode.SYSTEM]
     * preventing runtime crashes.
     *
     * **Default Values:**
     * - Theme Mode: [ThemeMode.SYSTEM]
     * - Dynamic Colors: `true`
     *
     * @return A [Flow] emitting the current persistent theme state.
     */
    val themePreferencesFlow: Flow<ThemePreferences> = context.dataStore.data.map { preferences ->
        val themeModeName = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name

        // Safely attempt to parse the string to an Enum
        val safeThemeMode = runCatching {
            ThemeMode.valueOf(themeModeName)
        }.getOrDefault(ThemeMode.SYSTEM)

        ThemePreferences(
            themeMode = safeThemeMode,
            useDynamicColors = preferences[Keys.USE_DYNAMIC_COLORS] ?: true
        )
    }

    /**
     * Persists a new theme mode selection.
     *
     * @param themeMode The [ThemeMode] to apply.
     * @throws IOException If an error occurs while writing to disk.
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    /**
     * Persists the preference for dynamic colors (Material You).
     *
     * @param useDynamicColors `true` to use wallpaper-derived colors.
     * @throws IOException If an error occurs while writing to disk.
     */
    suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_DYNAMIC_COLORS] = useDynamicColors
        }
    }
}
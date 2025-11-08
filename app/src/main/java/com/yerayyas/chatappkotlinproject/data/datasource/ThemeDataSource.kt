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

@Singleton
class ThemeDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLORS = booleanPreferencesKey("use_dynamic_colors")
    }

    val themePreferencesFlow: Flow<ThemePreferences> = context.dataStore.data.map { preferences ->
        ThemePreferences(
            themeMode = ThemeMode.valueOf(
                preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            ),
            useDynamicColors = preferences[Keys.USE_DYNAMIC_COLORS] ?: true
        )
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateDynamicColors(useDynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_DYNAMIC_COLORS] = useDynamicColors
        }
    }
}
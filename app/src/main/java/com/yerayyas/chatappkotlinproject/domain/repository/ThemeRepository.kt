package com.yerayyas.chatappkotlinproject.domain.repository

import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for application theme management and preferences.
 *
 * This interface defines the core contract for theme-related operations including
 * theme mode selection, dynamic color configuration, and reactive theme updates.
 * It provides a clean domain interface that abstracts the underlying persistence
 * mechanism (DataStore) from the business logic.
 *
 * Key responsibilities:
 * - **Theme Mode Management**: Handle Light, Dark, and System theme selection
 * - **Dynamic Colors**: Control Material You theming based on user wallpaper
 * - **Reactive Updates**: Provide real-time theme preference changes through Flow
 * - **Persistence**: Ensure theme preferences survive app restarts and updates
 *
 * The repository follows Clean Architecture principles by:
 * - Using domain models (ThemeMode, ThemePreferences) for type safety
 * - Providing reactive interfaces through Kotlin Flow for immediate UI updates
 * - Abstracting DataStore implementation details from domain logic
 * - Ensuring thread-safe operations for theme preference management
 */
interface ThemeRepository {

    /**
     * Reactive stream of current theme preferences.
     *
     * This Flow emits the complete theme configuration whenever any preference
     * changes, enabling the UI to react immediately to theme updates. The stream
     * provides both theme mode and dynamic color settings for comprehensive
     * theme management.
     *
     * Characteristics:
     * - Emits immediately with current preferences when collected
     * - Updates automatically when preferences change through other methods
     * - Thread-safe and suitable for observation from any coroutine context
     * - Maintains consistency across application components
     *
     * @return Flow of ThemePreferences containing current theme mode and dynamic color settings
     */
    val themePreferencesFlow: Flow<ThemePreferences>

    /**
     * Updates the application theme mode preference.
     *
     * This operation persists the new theme mode selection and triggers an update
     * in the themePreferencesFlow for immediate UI response. The change affects
     * the entire application's appearance and is preserved across app sessions.
     *
     * Supported theme modes:
     * - SYSTEM: Follow device system theme (automatic light/dark switching)
     * - LIGHT: Force light theme regardless of system setting
     * - DARK: Force dark theme regardless of system setting
     *
     * @param themeMode The new theme mode to apply and persist
     */
    suspend fun updateThemeMode(themeMode: ThemeMode)

    /**
     * Updates the dynamic colors (Material You) preference.
     *
     * This operation controls whether the application uses Material You theming
     * that adapts colors based on the user's wallpaper. The setting is persisted
     * and triggers updates in the themePreferencesFlow for immediate UI changes.
     *
     * Dynamic color behavior:
     * - true: Enable Material You theming with wallpaper-based colors (Android 12+)
     * - false: Use application's predefined color scheme regardless of wallpaper
     *
     * Note: Dynamic colors are only effective on Android 12+ devices. On older
     * devices, this setting is preserved but has no visual effect.
     *
     * @param useDynamicColors Whether to enable dynamic color theming based on wallpaper
     */
    suspend fun updateDynamicColors(useDynamicColors: Boolean)
}
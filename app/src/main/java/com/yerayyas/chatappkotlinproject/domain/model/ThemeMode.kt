/**
 * Domain model representing application theme configuration and preferences.
 *
 * This file contains the core theme-related domain models that define how the application
 * handles theming and appearance customization. These models are platform-agnostic and
 * represent pure business logic concepts for theme management.
 */
package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Enumeration of supported theme modes for the application.
 *
 * This enum defines the different theme options available to users, allowing them
 * to customize the application's appearance according to their preferences or
 * device settings.
 *
 * Theme modes supported:
 * - **SYSTEM**: Follows the device's system theme setting (Light/Dark)
 * - **LIGHT**: Forces light theme regardless of system setting
 * - **DARK**: Forces dark theme regardless of system setting
 *
 * The SYSTEM mode provides the best user experience by respecting the user's
 * device-wide theme preference and automatically adapting to system theme changes.
 */
enum class ThemeMode {
    /**
     * Follow system theme setting - automatically switches between light and dark
     * based on the device's current theme configuration.
     */
    SYSTEM,

    /**
     * Force light theme - uses light colors and themes regardless of system setting.
     * Provides consistent light appearance for users who prefer bright interfaces.
     */
    LIGHT,

    /**
     * Force dark theme - uses dark colors and themes regardless of system setting.
     * Provides consistent dark appearance for users who prefer darker interfaces.
     */
    DARK
}

/**
 * Data class representing comprehensive theme preferences for the application.
 *
 * This model encapsulates all theme-related user preferences and settings,
 * providing a complete configuration for the application's appearance and theming behavior.
 *
 * Key theme features:
 * - **Theme Mode Selection**: Choose between System, Light, or Dark themes
 * - **Dynamic Colors (Material You)**: Enable/disable wallpaper-based color theming
 * - **Reactive Updates**: Changes to these preferences trigger immediate UI updates
 * - **Persistent Storage**: Preferences are automatically saved using DataStore
 *
 * Default configuration:
 * - Theme follows system setting for better user experience
 * - Dynamic colors enabled to support Material You theming on compatible devices
 *
 * @property themeMode The selected theme mode (SYSTEM, LIGHT, or DARK)
 * @property useDynamicColors Whether to use Material You dynamic colors based on wallpaper
 */
data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true
)
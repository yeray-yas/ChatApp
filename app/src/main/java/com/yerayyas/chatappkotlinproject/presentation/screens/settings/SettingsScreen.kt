package com.yerayyas.chatappkotlinproject.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.theme.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themePreferences by themeViewModel.themePreferences.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            item {
                SettingsSection(
                    title = "Appearance",
                    icon = Icons.Default.Palette
                ) {
                    // Theme
                    SettingItem(
                        title = "Theme",
                        subtitle = when (themePreferences.themeMode) {
                            ThemeMode.SYSTEM -> "Follow system"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                        icon = Icons.Default.DarkMode,
                        onClick = { /* Will be handled with a dialog */ }
                    ) {
                        var showThemeDialog by remember { mutableStateOf(false) }

                        TextButton(
                            onClick = { showThemeDialog = true }
                        ) {
                            Text("Change")
                        }

                        if (showThemeDialog) {
                            ThemeSelectionDialog(
                                currentTheme = themePreferences.themeMode,
                                onThemeSelected = { theme ->
                                    themeViewModel.updateThemeMode(theme)
                                    showThemeDialog = false
                                },
                                onDismiss = { showThemeDialog = false }
                            )
                        }
                    }

                    // Dynamic colors (Android 12+)
                    SettingItem(
                        title = "Dynamic colors",
                        subtitle = "Use colors from wallpaper",
                        icon = Icons.Default.ColorLens
                    ) {
                        Switch(
                            checked = themePreferences.useDynamicColors,
                            onCheckedChange = { themeViewModel.updateDynamicColors(it) }
                        )
                    }
                }
            }

            // Notifications Section
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                ) {
                    var notificationsEnabled by remember { mutableStateOf(true) }
                    var soundEnabled by remember { mutableStateOf(true) }
                    var vibrationEnabled by remember { mutableStateOf(true) }

                    SettingItem(
                        title = "Push notifications",
                        subtitle = "Receive message notifications",
                        icon = Icons.Default.NotificationsActive
                    ) {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }

                    SettingItem(
                        title = "Sound",
                        subtitle = "Play sound when receiving messages",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        enabled = notificationsEnabled
                    ) {
                        Switch(
                            checked = soundEnabled && notificationsEnabled,
                            onCheckedChange = { soundEnabled = it },
                            enabled = notificationsEnabled
                        )
                    }

                    SettingItem(
                        title = "Vibration",
                        subtitle = "Vibrate when receiving messages",
                        icon = Icons.Default.Vibration,
                        enabled = notificationsEnabled
                    ) {
                        Switch(
                            checked = vibrationEnabled && notificationsEnabled,
                            onCheckedChange = { vibrationEnabled = it },
                            enabled = notificationsEnabled
                        )
                    }
                }
            }

            // Chat Section
            item {
                SettingsSection(
                    title = "Chat",
                    icon = Icons.AutoMirrored.Filled.Chat
                ) {
                    var autoDownloadImages by remember { mutableStateOf(true) }
                    var saveToGallery by remember { mutableStateOf(false) }

                    SettingItem(
                        title = "Auto download",
                        subtitle = "Download images automatically",
                        icon = Icons.Default.Download
                    ) {
                        Switch(
                            checked = autoDownloadImages,
                            onCheckedChange = { autoDownloadImages = it }
                        )
                    }

                    SettingItem(
                        title = "Save to gallery",
                        subtitle = "Save received images to gallery",
                        icon = Icons.Default.Photo
                    ) {
                        Switch(
                            checked = saveToGallery,
                            onCheckedChange = { saveToGallery = it }
                        )
                    }
                }
            }

            // Info Section
            item {
                SettingsSection(
                    title = "Information",
                    icon = Icons.Default.Info
                ) {
                    SettingItem(
                        title = "Version",
                        subtitle = "1.0.0 (Phase 2)",
                        icon = Icons.Default.AppSettingsAlt,
                        onClick = { /* Show changelog */ }
                    )

                    SettingItem(
                        title = "Developer",
                        subtitle = "Yeray Yas",
                        icon = Icons.Default.Person,
                        onClick = { /* Open profile */ }
                    )

                    SettingItem(
                        title = "Source code",
                        subtitle = "View on GitHub",
                        icon = Icons.Default.Code,
                        onClick = { /* Open GitHub */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    Card(
        onClick = onClick ?: {},
        enabled = enabled && onClick != null,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (action != null) {
                action()
            }
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = when (theme) {
                                ThemeMode.SYSTEM -> "Follow system"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
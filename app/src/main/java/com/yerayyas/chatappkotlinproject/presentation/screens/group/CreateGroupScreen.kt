package com.yerayyas.chatappkotlinproject.presentation.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.presentation.components.EmptyState
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.group.CreateGroupViewModel

/**
 * Screen for creating a new chat group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit, // Callback con el ID del grupo creado
    modifier: Modifier = Modifier,
    viewModel: CreateGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableUsers by viewModel.availableUsers.collectAsStateWithLifecycle()
    val selectedUsers by viewModel.selectedUsers.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Observar resultado de creación
    LaunchedEffect(uiState.isGroupCreated) {
        if (uiState.isGroupCreated) {
            // Por ahora usar un ID genérico, idealmente el ViewModel debería devolver el ID real
            onGroupCreated("group_created")
            viewModel.resetCreationState()
        }
    }

    // Mostrar errores
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Aquí podrías mostrar un SnackBar o Toast
            println("Error: $error")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Grupo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.createGroup() },
                        enabled = uiState.groupName.isNotBlank() &&
                                selectedUsers.isNotEmpty() &&
                                !uiState.isLoading
                    ) {
                        Text("Crear")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingState(
                    message = "Creando grupo...",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Información del grupo
                    item {
                        GroupInfoSection(
                            groupName = uiState.groupName,
                            onGroupNameChange = viewModel::updateGroupName,
                            groupDescription = uiState.groupDescription,
                            onGroupDescriptionChange = viewModel::updateGroupDescription
                        )
                    }

                    // Error
                    uiState.error?.let { error ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(onClick = viewModel::clearError) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cerrar error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Miembros seleccionados
                    if (selectedUsers.isNotEmpty()) {
                        item {
                            SelectedMembersSection(
                                selectedMembers = selectedUsers,
                                onRemoveMember = viewModel::toggleUserSelection
                            )
                        }
                    }

                    // Lista de usuarios disponibles
                    item {
                        Text(
                            text = "Agregar miembros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (availableUsers.isEmpty()) {
                        item {
                            EmptyState(
                                message = "No hay usuarios disponibles",
                                icon = Icons.Default.People
                            )
                        }
                    } else {
                        items(availableUsers) { user ->
                            UserSelectionItem(
                                user = user,
                                isSelected = selectedUsers.contains(user),
                                onSelectionChange = {
                                    viewModel.toggleUserSelection(user)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sección de información del grupo
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun GroupInfoSection(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    groupDescription: String,
    onGroupDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Nombre del grupo
            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Nombre del grupo") },
                placeholder = { Text("Ej: Familia, Trabajo, Amigos...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = groupName.length > 100
            )

            // Descripción del grupo
            OutlinedTextField(
                value = groupDescription,
                onValueChange = onGroupDescriptionChange,
                label = { Text("Descripción (opcional)") },
                placeholder = { Text("Describe de qué trata este grupo...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                isError = groupDescription.length > 500
            )
        }
    }
}

/**
 * Sección de miembros seleccionados
 */
@Composable
private fun SelectedMembersSection(
    selectedMembers: List<User>,
    onRemoveMember: (User) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = "Miembros",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Miembros seleccionados (${selectedMembers.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            selectedMembers.forEach { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar placeholder
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.username.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = user.username,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    IconButton(onClick = { onRemoveMember(user) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Item de selección de usuario
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun UserSelectionItem(
    user: User,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChange(!isSelected) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar del usuario
                if (user.profileImage.isNotEmpty()) {
                    GlideImage(
                        model = user.profileImage,
                        contentDescription = "Avatar de ${user.username}",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column {
                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (user.isOnline) "En línea" else "Desconectado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (user.isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        }
    }
}


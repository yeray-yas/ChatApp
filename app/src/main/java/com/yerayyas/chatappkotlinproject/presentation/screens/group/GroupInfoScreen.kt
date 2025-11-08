package com.yerayyas.chatappkotlinproject.presentation.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import com.yerayyas.chatappkotlinproject.data.model.User
import com.yerayyas.chatappkotlinproject.presentation.components.LoadingState
import com.yerayyas.chatappkotlinproject.presentation.components.ErrorState
import com.yerayyas.chatappkotlinproject.presentation.viewmodel.group.GroupListViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de información y gestión de un grupo específico
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    navController: NavController,
    viewModel: GroupListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Obtener información del grupo (por ahora usamos el ViewModel de lista)
    // En una implementación real, tendrías un ViewModel específico para GroupInfo

    LaunchedEffect(uiState.error, uiState.message) {
        val error = uiState.error
        val message = uiState.message

        if (error != null) {
            scope.launch {
                snackbarHostState.showSnackbar(error)
                viewModel.clearMessages()
            }
        } else if (message != null) {
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Información del Grupo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // TODO: Navegar a configuraciones del grupo
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuraciones"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: Añadir nuevos miembros
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Añadir miembro",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(
                        message = "Cargando información del grupo...",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.error != null -> {
                    ErrorState(
                        message = uiState.error ?: "Error desconocido",
                        onRetry = { viewModel.refreshGroups() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    // Por ahora mostramos datos mock hasta implementar el ViewModel específico
                    GroupInfoContent(
                        group = getMockGroup(groupId),
                        members = getMockMembers(),
                        currentUserId = getCurrentUserId(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Contenido principal de la información del grupo
 */
@Composable
private fun GroupInfoContent(
    group: GroupChat,
    members: List<User>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Información básica del grupo
        item {
            GroupBasicInfoCard(
                group = group,
                isAdmin = group.isAdmin(currentUserId)
            )
        }

        // Lista de miembros
        item {
            Text(
                text = "Miembros (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(members) { member ->
            MemberListItem(
                member = member,
                isAdmin = group.isAdmin(member.id),
                isCurrentUser = member.id == currentUserId,
                canManageMembers = group.isAdmin(currentUserId)
            )
        }

        // Espaciado para el FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Card con información básica del grupo
 */
@Composable
private fun GroupBasicInfoCard(
    group: GroupChat,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar del grupo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre del grupo
            Text(
                text = group.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción
            Text(
                text = group.description.ifEmpty { "Sin descripción" },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Información adicional
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                InfoItem(
                    label = "Miembros",
                    value = group.memberIds.size.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                InfoItem(
                    label = "Creado",
                    value = formatCreationDate(group.createdAt),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isAdmin) {
                    InfoItem(
                        label = "Rol",
                        value = "Admin",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Item de información
 */
@Composable
private fun InfoItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/**
 * Item de miembro en la lista
 */
@Composable
private fun MemberListItem(
    member: User,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    canManageMembers: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del miembro
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isCurrentUser) "${member.username} (Tú)" else member.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isAdmin) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (member.isOnline) "En línea" else "Desconectado",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (member.isOnline)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Acciones (solo para admins)
            if (canManageMembers && !isCurrentUser) {
                IconButton(
                    onClick = {
                        // TODO: Mostrar opciones de gestión de miembro
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gestionar miembro",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Formatea la fecha de creación
 */
private fun formatCreationDate(timestamp: Long): String {
    val date = Date(timestamp)
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
}

/**
 * Obtiene un grupo mock para demostración
 */
private fun getMockGroup(groupId: String): GroupChat {
    return GroupChat(
        id = groupId,
        name = "Grupo de Prueba",
        description = "Este es un grupo de prueba para la aplicación",
        memberIds = listOf("user1", "user2", "user3", "user4"),
        adminIds = listOf("user1"),
        createdBy = "user1",
        createdAt = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // Hace 7 días
        lastActivity = System.currentTimeMillis() - 3600000,
        isActive = true
    )
}

/**
 * Obtiene miembros mock para demostración
 */
private fun getMockMembers(): List<User> {
    return listOf(
        User(
            id = "user1",
            username = "juan_admin",
            email = "juan@example.com",
            profileImage = "",
            status = "online",
            isOnline = true
        ),
        User(
            id = "user2",
            username = "maria_garcia",
            email = "maria@example.com",
            profileImage = "",
            status = "online",
            isOnline = true
        ),
        User(
            id = "user3",
            username = "carlos_rodriguez",
            email = "carlos@example.com",
            profileImage = "",
            status = "offline",
            isOnline = false
        ),
        User(
            id = "user4",
            username = "ana_martinez",
            email = "ana@example.com",
            profileImage = "",
            status = "offline",
            isOnline = false
        )
    )
}

/**
 * Obtiene el ID del usuario actual
 */
private fun getCurrentUserId(): String {
    return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user1"
}
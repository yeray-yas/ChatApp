package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yerayyas.chatappkotlinproject.data.model.GroupChat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente que representa un elemento de chat grupal en la lista
 */
@Composable
fun GroupChatItem(
    groupChat: GroupChat,
    onGroupClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = { onGroupClick(groupChat.id) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del grupo
            GroupAvatar(
                groupImageUrl = groupChat.imageUrl,
                groupName = groupChat.name,
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Información del grupo
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nombre del grupo
                    Text(
                        text = groupChat.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Fecha de última actividad
                    Text(
                        text = formatTimestamp(groupChat.lastActivity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Información adicional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Último mensaje o descripción
                    Text(
                        text = groupChat.lastMessage?.message ?: groupChat.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Número de miembros
                    Text(
                        text = "${groupChat.memberIds.size} miembros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Avatar del grupo con ícono por defecto
 */
@Composable
private fun GroupAvatar(
    groupImageUrl: String?,
    groupName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Avatar por defecto con ícono de grupo (sin imagen por ahora)
        Card(
            modifier = Modifier.size(50.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "Grupo $groupName",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Formatea el timestamp para mostrar la fecha de forma amigable
 */
private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp > 0) {
        val date = Date(timestamp)
        val now = Date()
        val diffInMillis = now.time - date.time
        val diffInHours = diffInMillis / (1000 * 60 * 60)

        when {
            diffInHours < 1 -> "Ahora"
            diffInHours < 24 -> "${diffInHours}h"
            diffInHours < 48 -> "Ayer"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        }
    } else {
        ""
    }
}
package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yerayyas.chatappkotlinproject.data.model.ReadStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Indicador visual del estado del mensaje con animaciones
 */
@Composable
fun MessageStatusIndicator(
    readStatus: ReadStatus,
    timestamp: Long,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
    animated: Boolean = true
) {
    if (!isOwnMessage) return // Solo mostrar estado en mensajes propios

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showTime) {
            Text(
                text = formatTime(timestamp),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        AnimatedVisibility(
            visible = true,
            enter = if (animated) fadeIn() + scaleIn() else EnterTransition.None,
            exit = if (animated) fadeOut() + scaleOut() else ExitTransition.None
        ) {
            StatusIcon(
                readStatus = readStatus,
                animated = animated
            )
        }
    }
}

/**
 * Icono de estado con animaciones
 */
@Composable
private fun StatusIcon(
    readStatus: ReadStatus,
    animated: Boolean
) {
    val (icon, color, description) = when (readStatus) {
        ReadStatus.SENT -> Triple(
            Icons.Default.Done,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            "Enviado"
        )

        ReadStatus.DELIVERED -> Triple(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            "Entregado"
        )

        ReadStatus.READ -> Triple(
            Icons.Default.DoneAll,
            Color(0xFF00BFA5), // Verde WhatsApp-style
            "Leído"
        )
    }

    if (animated && readStatus == ReadStatus.READ) {
        // Animación especial para mensajes leídos
        val infiniteTransition = rememberInfiniteTransition(label = "read_glow")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_animation"
        )

        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier
                .size(16.dp)
                .alpha(alpha),
            tint = color
        )
    } else {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(16.dp),
            tint = color
        )
    }
}

/**
 * Indicador de estado expandido con información detallada
 */
@Composable
fun DetailedMessageStatus(
    readStatus: ReadStatus,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Estado del mensaje",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            StatusRow(
                icon = Icons.AutoMirrored.Filled.Send,
                label = "Enviado",
                time = formatDateTime(timestamp),
                isCompleted = true
            )

            StatusRow(
                icon = Icons.Default.DoneAll,
                label = "Entregado",
                time = if (readStatus != ReadStatus.SENT) formatDateTime(timestamp) else null,
                isCompleted = readStatus != ReadStatus.SENT
            )

            StatusRow(
                icon = Icons.Default.RemoveRedEye,
                label = "Leído",
                time = if (readStatus == ReadStatus.READ) formatDateTime(timestamp) else null,
                isCompleted = readStatus == ReadStatus.READ
            )
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    time: String?,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            if (time != null) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completado",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Indicador de estado de escritura (typing)
 */
@Composable
fun TypingIndicator(
    isVisible: Boolean,
    userName: String = "Usuario",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypingAnimation()

                Text(
                    text = "$userName está escribiendo...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Animación de puntos de escritura
 */
@Composable
private fun TypingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .alpha(alpha)
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Formatea el timestamp a formato de hora
 */
private fun formatTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

/**
 * Formatea el timestamp a formato de fecha y hora completo
 */
private fun formatDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
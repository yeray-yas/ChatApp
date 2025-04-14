package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.yerayyas.chatappkotlinproject.R
import com.yerayyas.chatappkotlinproject.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A composable that displays a user item in a list, typically used to represent
 * a chat contact or selectable user in the UI.
 *
 * @param user The [User] data to display in the list item.
 * @param onItemClick Callback to be triggered when the item is clicked.
 * @param modifier Optional [Modifier] for styling and layout.
 */
@Composable
fun UserListItem(
    user: User,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileImage(user.profileImage)
            UserInfoSection(user, Modifier.weight(1f))
        }
    }
}

/**
 * A composable that displays the online status or last seen timestamp for a user.
 *
 * @param user The [User] whose status is shown.
 */
@Composable
private fun ConnectionStatusRow(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OnlineStatusIndicator(isOnline = user.isOnline)
        Text(
            text = if (user.isOnline) "Online" else "Last seen: ${formatLastSeen(user.lastSeen)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Formats a timestamp into a human-readable date string.
 *
 * @param timestamp The last seen time in milliseconds.
 * @return A formatted string or "Never" if the timestamp is 0.
 */
private fun formatLastSeen(timestamp: Long): String {
    return if (timestamp == 0L) {
        "Never"
    } else {
        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        dateFormat.format(Date(timestamp))
    }
}

/**
 * A composable that displays a circular profile image.
 *
 * Uses a placeholder image in preview mode or when loading/failing to load.
 *
 * @param profileImage The URL of the image to display.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ProfileImage(profileImage: String) {
    if (LocalInspectionMode.current) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_background),
            contentDescription = "Preview",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        GlideImage(
            model = profileImage.takeIf { it.isNotEmpty() },
            contentDescription = "Avatar",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            failure = placeholder(R.drawable.ic_launcher_background),
            loading = placeholder(R.drawable.ic_launcher_background)
        )
    }
}

/**
 * A composable that displays the user's name, email, and connection status.
 *
 * @param user The [User] whose info is displayed.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
private fun UserInfoSection(user: User, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = user.username.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ConnectionStatusRow(user = user)
    }
}

/**
 * A small circular indicator to show whether the user is online (green) or offline (gray).
 *
 * @param isOnline Boolean indicating the online status.
 */
@Composable
private fun OnlineStatusIndicator(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(
                color = if (isOnline) Color.Green else Color.Gray.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.background,
                shape = CircleShape
            )
    )
}

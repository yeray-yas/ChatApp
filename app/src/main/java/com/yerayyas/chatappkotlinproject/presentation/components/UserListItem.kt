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

@Composable
private fun UserInfoSection(user: User, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = user.username.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT
                ) else it.toString()
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



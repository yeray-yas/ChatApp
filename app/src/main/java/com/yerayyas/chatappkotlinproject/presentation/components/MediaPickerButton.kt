package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MediaPickerButton(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String
) {
    var showPicker by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showPicker = true },
        modifier = modifier
            .size(40.dp)
            .background(Color.White, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select Media") },
            text = { Text("Choose an option from gallery or camera") },
            confirmButton = {
                Button(
                    onClick = {
                        showPicker = false
                        onGalleryClick()
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPicker = false
                        onCameraClick()
                    }
                ) {
                    Text("Camera")
                }
            }
        )
    }
}

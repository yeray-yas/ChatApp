package com.yerayyas.chatappkotlinproject.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A composable password input field with toggle visibility functionality.
 *
 * This component provides a secure text input field specifically designed for password entry.
 * It includes essential features for password input such as visual masking and visibility toggle,
 * along with appropriate keyboard configurations and visual indicators.
 *
 * Key features:
 * - Leading lock icon for visual context and accessibility
 * - Trailing visibility toggle button with dynamic icons
 * - Password masking with toggle functionality
 * - Appropriate keyboard type for password input
 * - IME action set to "Done" for better UX
 * - State preservation across configuration changes
 * - Customizable placeholder text
 * - Full-width layout by default
 *
 * The component automatically handles password visibility state and provides
 * appropriate content descriptions for accessibility.
 *
 * @param value The current text entered in the password field
 * @param onValueChange Callback invoked when the text changes
 * @param placeholder The placeholder text displayed when the field is empty
 * @param modifier A [Modifier] for styling and layout control
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = placeholder) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            PasswordVisibilityToggle(
                passwordVisible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible }
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password field"
            )
        }
    )
}

/**
 * Password visibility toggle icon button.
 *
 * This component renders an icon button that allows users to toggle
 * the visibility of password text. It provides appropriate visual
 * feedback and accessibility support.
 *
 * @param passwordVisible Current visibility state of the password
 * @param onToggleVisibility Callback invoked when the toggle is pressed
 */
@Composable
private fun PasswordVisibilityToggle(
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val image = if (passwordVisible) {
        Icons.Filled.Visibility
    } else {
        Icons.Filled.VisibilityOff
    }

    val description = if (passwordVisible) {
        "Hide password"
    } else {
        "Show password"
    }

    IconButton(onClick = onToggleVisibility) {
        Icon(
            imageVector = image,
            contentDescription = description
        )
    }
}

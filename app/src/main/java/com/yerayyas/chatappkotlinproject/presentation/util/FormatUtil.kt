package com.yerayyas.chatappkotlinproject.presentation.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Formats a message string to visually highlight mentions (text starting with '@').
 *
 * This function uses AnnotatedString to apply a different color and bold font
 * to mention tokens, improving readability in group chats.
 *
 * @param message The raw message string possibly containing mention tokens (e.g., @username).
 * @return An [AnnotatedString] with mentions styled.
 */
@Composable
fun formatMessageWithMentions(message: String): AnnotatedString {
    // Defines the regular expression pattern to find mentions (@followed by word characters)
    val mentionPattern = "@(\\w+)".toRegex()

    return buildAnnotatedString {
        var lastIndex = 0

        mentionPattern.findAll(message).forEach { matchResult ->
            // Append normal text before the mention
            append(message.substring(lastIndex, matchResult.range.first))

            // Apply special style for the mention
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary, // Primary color for mentions
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(matchResult.value)
            }

            lastIndex = matchResult.range.last + 1
        }

        // Append the rest of the text
        append(message.substring(lastIndex))
    }
}
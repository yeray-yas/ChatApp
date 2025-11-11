package com.yerayyas.chatappkotlinproject.utils.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Animation utilities for creating smooth hamburger-to-arrow menu icon transitions.
 *
 * This file contains composable functions that implement an animated hamburger menu icon
 * that transforms into an arrow icon when toggled. The animation follows Material Design
 * principles and provides smooth, intuitive visual feedback for navigation interactions.
 *
 * Key features:
 * - Smooth rotation and scaling animations
 * - Customizable colors and timing
 * - Material Design-compliant transitions
 * - Optimized performance with proper animation specifications
 * - Accessibility-friendly interaction patterns
 *
 * Architecture Pattern: UI Animation Components
 * - Reusable animation components following Compose best practices
 * - Declarative animation state management
 * - Configurable appearance and behavior
 * - Performance-optimized animation specifications
 * - Clean separation between animation logic and visual presentation
 *
 * Animation Details:
 * - Total animation duration: 600ms (200ms per phase)
 * - Three-phase animation: rotation, scaling, translation
 * - Uses tween interpolation for smooth transitions
 * - Transforms hamburger lines into arrow configuration
 * - Maintains visual consistency throughout transformation
 */

// Animation timing constants
private const val ANIMATION_DURATION_MS = 200
private const val ROTATION_DELAY_MS = 400
private const val SCALING_DELAY_MS = 200

// Visual constants  
private const val ARROW_ROTATION_DEGREES = 45f
private const val ARROW_ROTATION_DEGREES_NEGATIVE = -45f
private const val ARROW_SCALE_FACTOR = 0.5f
private const val LINE_SPACING_DP = 10f

// Component size constants
private val ICON_WIDTH = 65.dp
private val ICON_HEIGHT = 45.dp
private val ICON_CONTENT_WIDTH_FRACTION = 0.6f
private val LINE_HEIGHT = 2.dp

/**
 * Animated hamburger-to-arrow menu icon composable.
 *
 * This composable creates an interactive hamburger menu icon that smoothly animates
 * to an arrow icon when tapped. The animation consists of three coordinated phases:
 * rotation, scaling, and translation of the hamburger lines to form an arrow shape.
 *
 * Animation behavior:
 * - Tap to transform hamburger → arrow (600ms total duration)
 * - Tap again to transform arrow → hamburger (600ms total duration)
 * - Top line rotates 45° clockwise and moves to center
 * - Bottom line rotates 45° counterclockwise and moves to center
 * - Middle line scales down horizontally during transition
 *
 * Visual specifications:
 * - Icon size: 65dp × 45dp (follows Material Design touch target guidelines)
 * - Line thickness: 2dp (optimal for visibility and animation smoothness)
 * - Content area: 60% of icon width for proper visual balance
 * - Color: Customizable with default dark gray (#FF333333)
 *
 * Performance optimizations:
 * - Uses updateTransition for efficient animation state management
 * - Separates animation logic from rendering for better performance
 * - Proper use of remember for state persistence across recompositions
 * - Optimized transform origins for smooth rotation animations
 *
 * @param modifier Modifier to be applied to the root container (default: [Modifier])
 * @param color Color for the hamburger lines and overall icon (default: dark gray)
 * @param onToggle Callback invoked when the icon state changes (hamburger ↔ arrow)
 *
 * Example usage:
 * ```kotlin
 * HamburgerToArrowAnimation(
 *     modifier = Modifier.padding(16.dp),
 *     color = MaterialTheme.colors.onSurface,
 *     onToggle = { isArrow ->
 *         if (isArrow) openDrawer() else closeDrawer()
 *     }
 * )
 * ```
 */
@Composable
fun HamburgerToArrowAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF333333),
    onToggle: ((Boolean) -> Unit)? = null
) {
    // State management for animation
    var isMenuOpen by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = isMenuOpen, label = "menuTransition")

    // Calculate line positions using density-aware calculations
    val density = LocalDensity.current
    val topLineClosedY = with(density) { (-LINE_SPACING_DP).dp.toPx() }
    val bottomLineClosedY = with(density) { LINE_SPACING_DP.dp.toPx() }

    // Define animation specifications for each line (top and bottom)
    val lineAnimationSpecs = listOf(
        LineAnimationSpec(ARROW_ROTATION_DEGREES, topLineClosedY, "top"),
        LineAnimationSpec(ARROW_ROTATION_DEGREES_NEGATIVE, bottomLineClosedY, "bottom")
    )

    // Create animations for each line specification
    val lineAnimations = lineAnimationSpecs.map { spec ->
        LineAnimation(
            rotationAnimation = transition.animateFloat(
                transitionSpec = {
                    tween(ANIMATION_DURATION_MS, if (isMenuOpen) ROTATION_DELAY_MS else 0)
                },
                label = "${spec.label}LineRotation"
            ) { if (it) spec.targetRotation else 0f },

            scaleAnimation = transition.animateFloat(
                transitionSpec = { tween(ANIMATION_DURATION_MS, SCALING_DELAY_MS) },
                label = "${spec.label}LineScale"
            ) { if (it) ARROW_SCALE_FACTOR else 1f },

            translationAnimation = transition.animateFloat(
                transitionSpec = {
                    tween(ANIMATION_DURATION_MS, if (isMenuOpen) 0 else ROTATION_DELAY_MS)
                },
                label = "${spec.label}LineTranslation"
            ) { if (it) 0f else spec.initialTranslationY }
        )
    }

    // Handle click events
    val handleClick: () -> Unit = {
        isMenuOpen = !isMenuOpen
        onToggle?.invoke(isMenuOpen)
    }

    // Main icon container
    Box(
        modifier = modifier
            .size(ICON_WIDTH, ICON_HEIGHT)
            .clickable(onClick = handleClick),
        contentAlignment = Alignment.Center
    ) {
        // Animation container
        Box(
            modifier = Modifier
                .fillMaxWidth(ICON_CONTENT_WIDTH_FRACTION)
                .height(ICON_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            // Render animated top and bottom lines
            lineAnimations.forEach { animation ->
                AnimatedLine(
                    color = color,
                    rotation = animation.rotationAnimation.value,
                    scaleX = animation.scaleAnimation.value,
                    translationY = animation.translationAnimation.value,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Static middle line (always visible, serves as arrow shaft)
            MiddleLine(
                color = color,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Data class representing animation specifications for a single line.
 *
 * @property targetRotation Target rotation angle in degrees
 * @property initialTranslationY Initial Y translation in pixels
 * @property label Animation label for debugging and transition tracking
 */
private data class LineAnimationSpec(
    val targetRotation: Float,
    val initialTranslationY: Float,
    val label: String
)

/**
 * Data class containing all animations for a single line.
 *
 * @property rotationAnimation Animation state for rotation transformation
 * @property scaleAnimation Animation state for scaling transformation
 * @property translationAnimation Animation state for translation transformation
 */
private data class LineAnimation(
    val rotationAnimation: State<Float>,
    val scaleAnimation: State<Float>,
    val translationAnimation: State<Float>
)

/**
 * Renders an animated line with the specified transformations.
 *
 * This composable applies rotation, scaling, and translation transformations
 * to create the hamburger-to-arrow animation effect. It uses graphicsLayer
 * for optimal performance during animations.
 *
 * Transform details:
 * - Rotation: Applied around the left center of the line (transform origin: 0f, 0.5f)
 * - Scaling: Applied along X-axis to create arrow head effect
 * - Translation: Applied along Y-axis to move lines to center position
 *
 * @param color The color of the line
 * @param rotation Rotation angle in degrees
 * @param scaleX Horizontal scaling factor (1.0 = no scaling)
 * @param translationY Vertical translation in pixels
 * @param modifier Modifier to be applied to the line container
 */
@Composable
private fun AnimatedLine(
    color: Color,
    rotation: Float,
    scaleX: Float,
    translationY: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY
                this.scaleX = scaleX
                rotationZ = rotation
                transformOrigin = TransformOrigin(0f, 0.5f) // Rotate from left center
            }
            .fillMaxWidth()
            .height(LINE_HEIGHT)
            .background(color)
    )
}

/**
 * Renders the static middle line of the hamburger icon.
 *
 * This line remains visible throughout the animation and serves as the
 * arrow shaft when the icon transforms into arrow mode.
 *
 * @param color The color of the line
 * @param modifier Modifier to be applied to the line
 */
@Composable
private fun MiddleLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LINE_HEIGHT)
            .background(color)
    )
}

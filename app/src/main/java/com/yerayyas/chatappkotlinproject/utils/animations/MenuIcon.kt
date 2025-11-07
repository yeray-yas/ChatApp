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
 * A composable function that animates the transformation of a hamburger menu icon to an arrow icon.
 *
 * This function displays a hamburger menu icon that, when clicked, animates into an arrow icon and vice versa.
 * The transition is smooth, and the icon changes by rotating, scaling, and translating its lines.
 *
 * @param modifier A Modifier to be applied to the root Box element. Defaults to [Modifier].
 * @param color The color of the hamburger lines and background. Defaults to dark gray (0xFF333333).
 */
@Composable
fun HamburgerToArrowAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF333333)
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = isMenuOpen, label = "menuTransition")

    val density = LocalDensity.current
    val topLineClosedY = with(density) { (-10).dp.toPx() }
    val bottomLineClosedY = with(density) { 10.dp.toPx() }

    val lineSpecs = listOf(
        Triple(45f, topLineClosedY, "top"),
        Triple(-45f, bottomLineClosedY, "bottom")
    )

    val animations = lineSpecs.map { (rotation, initialY, label) ->
        Triple(
            transition.animateFloat(
                transitionSpec = { tween(200, if (isMenuOpen) 400 else 0) },
                label = "${label}LineRotation"
            ) { if (it) rotation else 0f },
            transition.animateFloat(
                transitionSpec = { tween(200, 200) },
                label = "${label}LineWidth"
            ) { if (it) 0.5f else 1f },
            transition.animateFloat(
                transitionSpec = { tween(200, if (isMenuOpen) 0 else 400) },
                label = "${label}LineY"
            ) { if (it) 0f else initialY }
        )
    }

    Box(
        modifier = modifier
            .size(65.dp, 45.dp)
            .clickable { isMenuOpen = !isMenuOpen }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(45.dp)
                .align(Alignment.Center)
        ) {
            animations.forEachIndexed { _, (rotation, width, translationY) ->
                Line(color, rotation.value, width.value, translationY.value, modifier = Modifier.align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(color)
            )
        }
    }
}

/**
 * A helper composable function that renders a line for the hamburger-to-arrow animation.
 *
 * This function takes care of rendering each line that is part of the hamburger icon, applying the necessary
 * transformations (rotation, scaling, and translation) based on the animation state.
 *
 * @param color The color of the line.
 * @param rotation The rotation applied to the line, in degrees.
 * @param scaleX The scaling factor applied to the line along the X-axis.
 * @param translationY The vertical translation applied to the line.
 * @param modifier A Modifier to be applied to the line. Defaults to [Modifier].
 */
@Composable
private fun Line(color: Color, rotation: Float, scaleX: Float, translationY: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationY = translationY
                this.scaleX = scaleX
                rotationZ = rotation
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
            .fillMaxWidth()
            .height(2.dp)
            .background(color)
    )
}

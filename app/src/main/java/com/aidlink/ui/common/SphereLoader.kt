package com.aidlink.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SphereLoader(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere_loader_transition")

    // A single clockwise rotation for the entire sphere.
    // A positive targetValue (360f) ensures clockwise rotation.
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sphere_rotation"
    )

    val dash = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

    // The parent Box handles the unified rotation and 3D tilt for the whole sphere.
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = rotation // Apply the clockwise rotation here
                rotationY = 20f      // A slight static tilt for a 3D perspective
            },
        contentAlignment = Alignment.Center
    ) {
        // The Canvas elements now just draw the static shapes of the sphere.
        // The parent Box handles all the animation.

        // Orbit 1 (Tilted 45 degrees)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = 45f }
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }

        // Orbit 2 (Tilted -45 degrees)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = -45f }
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }

        // Orbit 3 (Flat "Equator")
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }
    }
}
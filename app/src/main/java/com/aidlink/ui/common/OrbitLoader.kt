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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OrbitLoader(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_loader_transition")

    // A positive targetValue creates a clockwise rotation.
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    val dash = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationY = 20f }, // A slight tilt for a 3D perspective
        contentAlignment = Alignment.Center
    ) {
        // First Orbit (Tilted on Y-axis, rotating on Z-axis)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    rotationY = 45f
                }
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }

        // Second Orbit (Tilted on Y-axis the other way, rotating on Z-axis)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    rotationY = -45f
                }
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }

        // Third Orbit (Flat, rotating on Z-axis)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = -rotation // Counter-rotation for visual complexity
                }
        ) {
            drawOval(
                color = Color.White.copy(alpha = 0.8f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = dash)
            )
        }
    }
}
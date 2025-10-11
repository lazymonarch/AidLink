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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ThreeDOrbitLoader(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit_transition")

    val rotationX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 3000, easing = LinearEasing)
        ), label = "orbit_rotation_x"
    )
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 5000, easing = LinearEasing)
        ), label = "orbit_rotation_y"
    )

    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Box(
        modifier = modifier
            .size(size)
            // --- THIS IS THE FIX ---
            // Use 'this.rotationX' to refer to the graphicsLayer's property
            .graphicsLayer { this.rotationX = 25f }, // tilt for 3D depth
        contentAlignment = Alignment.Center
    ) {
        // "Up" orbit (rotates around X axis)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.rotationX = rotationX } // This correctly uses the animation variable
        ) {
            val stroke = Stroke(width = 1.2.dp.toPx(), pathEffect = dash)
            drawOrbitSet(stroke, size.toPx())
        }

        // "Side" orbit (rotates around Y axis)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { this.rotationY = rotationY } // This correctly uses the animation variable
        ) {
            val stroke = Stroke(width = 1.2.dp.toPx(), pathEffect = dash)
            drawOrbitSet(stroke, size.toPx())
        }
    }
}

// Helper: draw 3 concentric dashed ellipses
private fun DrawScope.drawOrbitSet(stroke: Stroke, totalSizePx: Float) {
    drawOval(
        color = Color.White.copy(alpha = 0.7f),
        style = stroke
    )
    drawOval(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(totalSizePx * 0.1f, 0f),
        size = Size(width = size.width * 0.8f, height = size.height),
        style = stroke
    )
    drawOval(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(totalSizePx * 0.25f, 0f),
        size = Size(width = size.width * 0.5f, height = size.height),
        style = stroke
    )
}
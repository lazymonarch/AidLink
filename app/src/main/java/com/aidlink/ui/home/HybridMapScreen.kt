package com.aidlink.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.model.HelpRequest
import com.aidlink.viewmodel.HomeViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridMapScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val requests by homeViewModel.requests.collectAsState()
    val userGeoPoint by homeViewModel.userGeoPoint.collectAsState()
    val scope = rememberCoroutineScope()

    // Default to Chennai if no user location
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(80.2707, 13.0827)) // Default Chennai
            zoom(13.0) // Zoom for 10km visibility
        }
    }

    // Automatically center map on user location when it becomes available
    LaunchedEffect(userGeoPoint) {
        userGeoPoint?.let { location ->
            scope.launch {
                mapViewportState.flyTo(
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                        .zoom(13.0) // 10km visibility
                        .build(),
                    animationOptions = null // Use default animation
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map takes most of the screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                // User location marker - Green Circle
                userGeoPoint?.let { userLocation ->
                    CircleAnnotationGroup(
                        annotations = listOf(
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                                .withCircleRadius(12.0)
                                .withCircleColor("#4CAF50")
                                .withCircleStrokeColor("#FFFFFF")
                                .withCircleStrokeWidth(3.0)
                                .withCircleOpacity(0.9)
                        )
                    )

                    // User location text label
                    PointAnnotationGroup(
                        annotations = listOf(
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                                .withTextField("📍 Your Location")
                                .withTextOffset(listOf(0.0, -2.5))
                                .withTextSize(12.0)
                                .withTextColor("#4CAF50")
                                .withTextHaloColor("#FFFFFF")
                                .withTextHaloWidth(2.0)
                        )
                    )
                }

                // Request markers - Red Circles
                if (requests.isNotEmpty()) {
                    CircleAnnotationGroup(
                        annotations = requests.map { request ->
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(request.longitude, request.latitude))
                                .withCircleRadius(10.0)
                                .withCircleColor("#FF5722")
                                .withCircleStrokeColor("#FFFFFF")
                                .withCircleStrokeWidth(2.0)
                                .withCircleOpacity(0.9)
                        }
                    )

                    // Request text labels
                    PointAnnotationGroup(
                        annotations = requests.map { request ->
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(request.longitude, request.latitude))
                                .withTextField("🆘 ${request.title}")
                                .withTextOffset(listOf(0.0, -2.5))
                                .withTextSize(10.0)
                                .withTextColor("#FF5722")
                                .withTextHaloColor("#FFFFFF")
                                .withTextHaloWidth(2.0)
                        }
                    )
                }
            }

            // Map Controls - Zoom and My Location
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Zoom In Button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .zoom(minOf(currentZoom + 1, 18.0))
                                    .build()
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Out Button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder()
                                    .zoom(maxOf(currentZoom - 1, 1.0))
                                    .build()
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    contentColor = Color.Black
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // My Location Button
                FloatingActionButton(
                    onClick = {
                        userGeoPoint?.let { location ->
                            scope.launch {
                                mapViewportState.flyTo(
                                    cameraOptions = CameraOptions.Builder()
                                        .center(Point.fromLngLat(location.longitude, location.latitude))
                                        .zoom(13.0)
                                        .build()
                                )
                            }
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Floating Action Button positioned on the map
            FloatingActionButton(
                onClick = { navController.navigate("post_request") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF03A9F4),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Request")
            }
        }

        // Horizontal scrollable request cards at the bottom
        if (requests.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Nearby Requests",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(requests) { request ->
                        HorizontalRequestCard(
                            request = request,
                            onClick = {
                                navController.navigate("request_detail/${request.id}")
                            }
                        )
                    }
                }
            }
        } else {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No nearby requests found.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun HorizontalRequestCard(request: HelpRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = rememberRipple()
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text(
                text = request.title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = request.description,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row with location and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location
                Text(
                    text = request.locationName,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Type badge
                val typeColor = if (request.type.name == "FEE") Color(0xFF4CAF50) else Color(0xFF2196F3)
                val typeText = if (request.type.name == "FEE") "Paid" else "Free"

                Surface(
                    color = typeColor.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = typeText,
                        color = typeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
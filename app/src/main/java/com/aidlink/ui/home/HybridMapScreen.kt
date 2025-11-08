package com.aidlink.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.R
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
    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    // Default to Chennai if no user location
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(80.2707, 13.0827)) // Default Chennai
            zoom(14.0) // Higher zoom for 5km visibility
        }
    }

    // Automatically center map on user location when it becomes available
    LaunchedEffect(userGeoPoint) {
        userGeoPoint?.let { location ->
            scope.launch {
                mapViewportState.flyTo(
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                        .zoom(14.0)
                        .build(),
                    animationOptions = null // Use default animation
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(max = 300.dp)
                ) {
                    Text("Nearby Requests", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(requests) { request ->
                            RequestCard(request = request, onClick = {
                                navController.navigate("request_detail/${request.id}")
                            })
                        }
                    }
                }
            },
            sheetPeekHeight = 120.dp,
            containerColor = Color.Black
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                MapboxMap(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
                if (requests.isEmpty()) {
                    Text(
                        "No nearby requests found.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
            }
        }
        // Floating Action Button positioned above the bottom sheet
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
}

@Composable
private fun RequestCard(request: HelpRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = rememberRipple()
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(request.title, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text(request.locationName, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// In: ui/home/HybridMapScreen.kt
package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.ui.theme.FeeGreen
import com.aidlink.ui.theme.VolunteerBlue
import com.aidlink.viewmodel.HomeViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import kotlinx.coroutines.launch

// Helper to convert Compose Color to the Hex String Mapbox needs
private fun Color.toHexString(): String {
    return String.format(
        "#%02X%02X%02X",
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridMapScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val requests by homeViewModel.requests.collectAsState()
    val userGeoPoint by homeViewModel.userGeoPoint.collectAsState()
    val scope = rememberCoroutineScope()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(80.2707, 13.0827)) // Default Chennai
            zoom(13.0)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            confirmValueChange = {
                it != SheetValue.Hidden
            }
        )
    )

    LaunchedEffect(userGeoPoint) {
        userGeoPoint?.let { location ->
            scope.launch {
                mapViewportState.flyTo(
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                        .zoom(13.0)
                        .build(),
                    animationOptions = null
                )
            }
        }
    }

    val dragHandleInteractionSource = remember { MutableInteractionSource() }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            RequestSheetContent(
                requests = requests,
                onItemClick = { request ->
                    navController.navigate("request_detail/${request.id}")
                    scope.launch {
                        mapViewportState.flyTo(
                            cameraOptions = CameraOptions.Builder()
                                .center(Point.fromLngLat(request.longitude, request.latitude))
                                .zoom(14.0)
                                .build()
                        )
                    }
                }
            )
        },
        sheetPeekHeight = 60.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetDragHandle = {
            CustomDragHandle(interactionSource = dragHandleInteractionSource)
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                val volunteerColorHex = VolunteerBlue.toHexString()
                val errorColorHex = MaterialTheme.colorScheme.error.toHexString()
                val surfaceColorHex = MaterialTheme.colorScheme.surface.toHexString()

                userGeoPoint?.let { userLocation ->
                    CircleAnnotationGroup(
                        annotations = listOf(
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                                .withCircleRadius(12.0)
                                .withCircleColor(volunteerColorHex)
                                .withCircleStrokeColor(surfaceColorHex)
                                .withCircleStrokeWidth(3.0)
                                .withCircleOpacity(0.9)
                        )
                    )
                }

                if (requests.isNotEmpty()) {
                    CircleAnnotationGroup(
                        annotations = requests.map { request ->
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(request.longitude, request.latitude))
                                .withCircleRadius(10.0)
                                .withCircleColor(errorColorHex)
                                .withCircleStrokeColor(surfaceColorHex)
                                .withCircleStrokeWidth(2.0)
                        }
                    )
                }
            }

            // Map Controls
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder().zoom(minOf(currentZoom + 1, 18.0)).build()
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                }

                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder().zoom(maxOf(currentZoom - 1, 1.0)).build()
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
                }
            }
             FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                onClick = { navController.navigate("post_request") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Request")
            }
        }
    }
}

@Composable
private fun CustomDragHandle(interactionSource: MutableInteractionSource) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val handleColor = if (isPressed) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(handleColor, shape = RoundedCornerShape(2.dp))
        )
    }
}


@Composable
private fun RequestSheetContent(
    requests: List<HelpRequest>,
    onItemClick: (HelpRequest) -> Unit
) {
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .padding(start = 16.dp, end = 16.dp)
            .nestedScroll(nestedScrollConnection)
    ) {
        Text(
            text = "Nearby Requests",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        if (requests.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = requests, key = { it.id }) { request ->
                    VerticalRequestCard(
                        request = request,
                        onClick = { onItemClick(request) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No nearby requests found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun VerticalRequestCard(request: HelpRequest, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = request.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                val typeColor = if (request.type == RequestType.FEE) FeeGreen else VolunteerBlue
                val typeText = if (request.type == RequestType.FEE) "Fee" else "Volunteer"
                AssistChip(
                    onClick = { /* no-op */ },
                    label = {
                        Text(
                            text = typeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = typeColor.copy(alpha = 0.1f),
                        labelColor = typeColor
                    ),
                    border = null
                )
            }
        }
    }
}

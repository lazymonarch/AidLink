
package com.aidlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.viewmodel.HomeViewModel
// Import GeoPoint and Geometries
import com.google.firebase.firestore.GeoPoint
import com.aidlink.utils.Geometries
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridMapScreen(
    navController: NavController,
    homeViewModel: HomeViewModel
) {
    val requests by homeViewModel.requests.collectAsState()
    val userGeoPoint by homeViewModel.userGeoPoint.collectAsState()
    val centerMapOnUserAction by homeViewModel.centerMapOnUserAction.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("All") }

    val mapViewportState = homeViewModel.mapViewportState

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            skipHiddenState = true
        )
    )

    // Handle the one-time centering action from the ViewModel
    LaunchedEffect(Unit) {
        homeViewModel.centerMapOnUserAction.collectLatest { geoPoint ->
            geoPoint?.let {
                mapViewportState.flyTo(
                    cameraOptions = CameraOptions.Builder()
                        .center(Point.fromLngLat(it.longitude, it.latitude))
                        .zoom(13.0)
                        .build(),
                    animationOptions = null
                )
                // Consume the event
                homeViewModel.onMapCentered()
            }
        }
    }

    // Conditionally reset the map's zoom when the screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val currentZoom = mapViewportState.cameraState?.zoom
                if (currentZoom != null && (currentZoom < 10.0 || currentZoom > 16.0)) {
                    homeViewModel.resetMapToUserLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            RequestsBottomSheetContent(
                requests = requests,
                userGeoPoint = userGeoPoint,
                onRequestClick = { requestId ->
                    navController.navigate("request_detail/$requestId")
                }
            )
        },
        sheetPeekHeight = 90.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = Color(0xFFFAFAFA),
        sheetTonalElevation = 8.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = Color(0xFFCAC4D0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = {
                    MapboxStandardStyle()
                }
            ) {
                CircleAnnotationGroup(
                    requests.map { request ->
                        CircleAnnotationOptions()
                            .withPoint(Point.fromLngLat(request.longitude, request.latitude))
                            .withCircleColor(
                                when (request.type) {
                                    RequestType.FEE -> "#FF6B35"
                                    RequestType.VOLUNTEER -> "#00897B"
                                }
                            )
                            .withCircleRadius(12.0)
                            .withCircleStrokeColor("#FFFFFF")
                            .withCircleStrokeWidth(2.0)
                    }
                )

                userGeoPoint?.let { geoPoint ->
                    CircleAnnotationGroup(
                        listOf(
                            CircleAnnotationOptions()
                                .withPoint(Point.fromLngLat(geoPoint.longitude, geoPoint.latitude))
                                .withCircleColor("#4A80F5")
                                .withCircleRadius(10.0)
                                .withCircleStrokeColor("#FFFFFF")
                                .withCircleStrokeWidth(2.5)
                                .withCircleStrokeColor("#1E3A75")
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: Open search */ }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF49454F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search requests...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF79747E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { /* TODO: Show filter options */ },
                    label = { Text("All") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B35),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 150.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder().zoom(minOf(currentZoom + 1, 18.0)).build()
                            )
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    contentColor = Color(0xFF1C1B1F),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom in")
                }

                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                            mapViewportState.flyTo(
                                cameraOptions = CameraOptions.Builder().zoom(maxOf(currentZoom - 1, 1.0)).build()
                            )
                        }
                     },
                    modifier = Modifier.size(48.dp),
                    containerColor = Color.White,
                    contentColor = Color(0xFF1C1B1F)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom out")
                }

                Spacer(modifier = Modifier.height(8.dp))

                FloatingActionButton(
                    onClick = { homeViewModel.resetMapToUserLocation() },
                    modifier = Modifier.size(56.dp),
                    containerColor = Color(0xFFE8DEF8),
                    contentColor = Color(0xFF1C1B1F)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My location",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            ExtendedFloatingActionButton(
                onClick = { navController.navigate("post_request") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFFFF6B35),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Post Request",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RequestsBottomSheetContent(
    requests: List<HelpRequest>,
    userGeoPoint: GeoPoint?,
    onRequestClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Requests",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F),
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFE5DD),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${requests.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (requests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Color(0xFFCAC4D0),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No requests nearby",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF49454F)
                )
                Text(
                    text = "Try adjusting your location or filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF79747E)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests) { request ->
                    RequestCard(
                        request = request,
                        userGeoPoint = userGeoPoint,
                        onClick = { onRequestClick(request.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun RequestCard(
    request: HelpRequest,
    userGeoPoint: GeoPoint?,
    onClick: () -> Unit
) {
    val distanceText = remember(userGeoPoint, request) {
        if (userGeoPoint != null) {
            try {
                val userLocation = Geometries.point(userGeoPoint.latitude, userGeoPoint.longitude)
                val requestLocation = Geometries.point(request.latitude, request.longitude)
                val distanceKm = userLocation.distance(requestLocation)
                String.format("%.1f km", distanceKm)
            } catch (e: Exception) {
                "N/A"
            }
        } else {
            "..."
        }
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (request.type) {
                        RequestType.FEE -> Color(0xFFFFE5DD)
                        RequestType.VOLUNTEER -> Color(0xFFE0F2F1)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (request.type) {
                                RequestType.FEE -> Icons.Default.AttachMoney
                                RequestType.VOLUNTEER -> Icons.Default.Favorite
                            },
                            contentDescription = null,
                            tint = when (request.type) {
                                RequestType.FEE -> Color(0xFFFF6B35)
                                RequestType.VOLUNTEER -> Color(0xFF00897B)
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (request.type) {
                                RequestType.FEE -> "Fee"
                                RequestType.VOLUNTEER -> "Free"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (request.type) {
                                RequestType.FEE -> Color(0xFFFF6B35)
                                RequestType.VOLUNTEER -> Color(0xFF00897B)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF49454F),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF79747E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = request.locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF79747E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF79747E),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF79747E)
                )

                Spacer(modifier = Modifier.width(12.dp))

                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = request.category,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(28.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFF5F5F5),
                        labelColor = Color(0xFF49454F)
                    ),
                    border = null
                )
            }
        }
    }
}

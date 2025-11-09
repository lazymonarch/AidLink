package com.aidlink.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.viewmodel.HomeViewModel
import com.aidlink.viewmodel.RespondUiState
import com.aidlink.model.Offer
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotationGroup
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    homeViewModel: HomeViewModel,
    onBackClicked: () -> Unit
) {
    val request by homeViewModel.selectedRequest.collectAsState()
    val offers by homeViewModel.offers.collectAsState()
    val uiState by homeViewModel.respondUiState.collectAsState()
    val currentUser = Firebase.auth.currentUser

    DisposableEffect(Unit) {
        onDispose {
            homeViewModel.resetRespondState()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Request Details", color = Color.White) },
                navigationIcon = { 
                    IconButton(onClick = onBackClicked) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) 
                    } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        bottomBar = {
            if (request != null && currentUser != null) {
                if (request!!.userId == currentUser.uid) {
                    RequesterBottomBar(offers = offers, onAccept = { /* Handled in MyActivityScreen */ })
                } else {
                    val hasAlreadyOffered = offers.any { it.helperId == currentUser.uid }
                    HelperBottomBar(
                        uiState = uiState,
                        hasAlreadyOffered = hasAlreadyOffered,
                        onMakeOffer = { homeViewModel.onMakeOffer(request!!.id) }
                    )
                }
            }
        }
    ) { innerPadding ->
        request?.let {
            RequestDetailsContent(it, innerPadding)
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun HelperBottomBar(
    uiState: RespondUiState,
    hasAlreadyOffered: Boolean,
    onMakeOffer: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasAlreadyOffered -> {
                Text("Offer Sent!", color = Color.Green, fontWeight = FontWeight.Bold)
            }
            uiState is RespondUiState.Loading -> CircularProgressIndicator(color = Color.White)
            uiState is RespondUiState.Success -> Text("Offer Sent!", color = Color.Green, fontWeight = FontWeight.Bold)
            uiState is RespondUiState.Error -> Text(uiState.message, color = MaterialTheme.colorScheme.error)
            else -> {
                Button(
                    onClick = onMakeOffer,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("I Can Help", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RequesterBottomBar(offers: List<Offer>, onAccept: (Offer) -> Unit) {
    Surface(color = Color.Black, tonalElevation = 4.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (offers.isEmpty()) "No offers yet." else "${offers.size} offer(s) received.",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "You can manage offers from the 'My Activity' screen.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RequestDetailsContent(request: HelpRequest, innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header Card with Title and User Info
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Title
                Text(
                    text = request.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // User Info Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // User Avatar Placeholder
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = request.userName.ifEmpty { "Anonymous User" },
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        
                        // Timestamp
                        request.timestamp?.let { timestamp ->
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                            Text(
                                text = dateFormat.format(timestamp.toDate()),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Description",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = request.description.ifEmpty { "No description provided." },
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category
                DetailRowWithIcon(
                    icon = Icons.Default.Category,
                    label = "Category",
                    value = request.category.ifEmpty { "General" }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Compensation Type
                val compensationText = if (request.type == RequestType.FEE) "Paid Help" else "Volunteer"
                val compensationColor = if (request.type == RequestType.FEE) Color(0xFF4CAF50) else Color(0xFF2196F3)
                
                DetailRowWithIcon(
                    icon = Icons.Default.AttachMoney,
                    label = "Type",
                    value = compensationText,
                    valueColor = compensationColor
                )
                
                // Show compensation amount if it's a paid request
                if (request.type == RequestType.FEE && request.compensation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRowWithIcon(
                        icon = Icons.Default.AttachMoney,
                        label = "Amount",
                        value = request.compensation,
                        valueColor = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Location Map Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Location",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Mini Map
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    RequestLocationMap(request = request)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Location Name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = request.locationName.ifEmpty { "Location not specified" },
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Add some bottom padding for the floating action button
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun DetailRowWithIcon(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RequestLocationMap(request: HelpRequest) {
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(request.longitude, request.latitude))
            zoom(15.0) // Closer zoom for detail view
        }
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = mapViewportState
    ) {
        // Request location marker
        CircleAnnotationGroup(
            annotations = listOf(
                CircleAnnotationOptions()
                    .withPoint(Point.fromLngLat(request.longitude, request.latitude))
                    .withCircleRadius(12.0)
                    .withCircleColor("#FF5722")
                    .withCircleStrokeColor("#FFFFFF")
                    .withCircleStrokeWidth(3.0)
                    .withCircleOpacity(0.9)
            )
        )
    }
}

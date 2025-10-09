package com.aidlink.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onPostRequestClicked: () -> Unit,
    onRequestClicked: (String) -> Unit
) {
    val requests by homeViewModel.requests.collectAsState()

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AidLink", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: Handle filter */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.LightGray
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onPostRequestClicked,
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post a request")
            }
        }
    ) { innerPadding ->
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No help requests in your area yet. Be the first to post one!",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(requests) { request ->
                    HelpRequestCard(
                        request = request,
                        onClick = { onRequestClicked(request.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpRequestCard(
    request: HelpRequest,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp
                )
                Surface(shape = RoundedCornerShape(50), color = Color.DarkGray) {
                    Text(
                        text = request.category,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(icon = Icons.Default.LocationOn, text = request.location)

                val (icon, text, color) = when(request.type) {
                    RequestType.VOLUNTEER -> Triple(Icons.Default.VolunteerActivism, "Volunteer", Color(0xFF4DABF7))
                    RequestType.FEE -> Triple(Icons.Default.Payments, "Fee", Color(0xFFFF8A65))
                }
                InfoRow(icon = icon, text = text, iconColor = color)
            }
            Text(
                text = request.description,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, iconColor: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    // --- THIS IS THE CORRECTED SECTION ---
    val previewRequests = listOf(
        HelpRequest(
            id = "1",
            userId = "", // <-- ADDED DUMMY USER ID
            title = "Need help setting up my new router",
            description = "I'm not very tech-savvy...",
            category = "Tech",
            location = "Approx. 0.5 miles away",
            type = RequestType.FEE,
            status = "open"
        ),
        HelpRequest(
            id = "2",
            userId = "", // <-- ADDED DUMMY USER ID
            title = "Grocery shopping assistance",
            description = "I'm recovering from a minor surgery...",
            category = "Shopping",
            location = "Approx. 1.2 miles away",
            type = RequestType.FEE,
            status = "open"
        ),
        HelpRequest(
            id = "3",
            userId = "", // <-- ADDED DUMMY USER ID
            title = "Dog walking needed",
            description = "I have an energetic labrador...",
            category = "Pet Care",
            location = "Approx. 0.8 miles away",
            type = RequestType.VOLUNTEER,
            status = "completed"
        )
    )
    AidLinkTheme(darkTheme = true) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(previewRequests) { request ->
                HelpRequestCard(
                    request = request,
                    onClick = {}
                )
            }
        }
    }
}
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
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = request.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = request.description, color = Color.LightGray, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.DarkGray)
                DetailRow(label = "Category", value = request.category)
                HorizontalDivider(color = Color.DarkGray)
                DetailRow(label = "Location", value = request.locationName)
                HorizontalDivider(color = Color.DarkGray)
                val compensationText = if (request.type == RequestType.FEE) "Fee" else "Volunteer"
                DetailRow(label = "Compensation", value = compensationText)
            }
        }
    }
}


@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 16.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
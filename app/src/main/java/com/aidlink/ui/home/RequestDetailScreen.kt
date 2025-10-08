package com.aidlink.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel
import com.aidlink.viewmodel.RequestUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    homeViewModel: HomeViewModel,
    onBackClicked: () -> Unit
) {
    val request by homeViewModel.selectedRequest.collectAsState()
    val requestUiState by homeViewModel.requestUiState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            homeViewModel.resetRequestState()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Request Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // The bottom bar now reacts to the UI state
                when (requestUiState) {
                    is RequestUiState.Loading -> {
                        CircularProgressIndicator(color = Color.White)
                    }
                    is RequestUiState.Success -> {
                        Text("Offer Sent!", color = Color.Green, fontWeight = FontWeight.Bold)
                    }
                    is RequestUiState.Error -> {
                        Text("Something went wrong.", color = MaterialTheme.colorScheme.error)
                    }
                    is RequestUiState.Idle -> {
                        Button(
                            onClick = {
                                request?.id?.let {
                                    homeViewModel.onRespondToRequest(it)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("I Can Help", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        request?.let {
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
                        Text(
                            text = it.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = it.description,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        DetailRow(label = "Category", value = it.category)
                        Divider(color = Color.DarkGray)
                        DetailRow(label = "Location", value = it.location)
                        Divider(color = Color.DarkGray)
                        val compensationText = when(it.type) {
                            RequestType.VOLUNTEER -> "Volunteer"
                            RequestType.FEE -> "Fee"
                        }
                        DetailRow(label = "Compensation", value = compensationText)
                    }
                }
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

@Preview
@Composable
fun RequestDetailScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        // This preview is static and won't work with a live ViewModel
        // For a working preview, you'd pass a fake ViewModel
    }
}
package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.model.HelpRequest
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.MyActivityViewModel
import com.aidlink.viewmodel.RequestUiState
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyActivityScreen(
    myActivityViewModel: MyActivityViewModel = viewModel()
) {
    val tabTitles = listOf("My Requests", "My Responses", "Completed")
    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    val myRequests by myActivityViewModel.myRequests.collectAsState()
    val myResponses by myActivityViewModel.myResponses.collectAsState()
    val completedRequests by myActivityViewModel.completedRequests.collectAsState()

    // NEW: This state now holds the request to show in the dialog
    var requestToShowInDialog by remember { mutableStateOf<HelpRequest?>(null) }
    val actionUiState by myActivityViewModel.actionUiState.collectAsState()

    // --- NEW: The Floating Card Dialog ---
    if (requestToShowInDialog != null) {
        OfferManagementDialog(
            request = requestToShowInDialog!!,
            actionUiState = actionUiState,
            onDismiss = {
                requestToShowInDialog = null
                myActivityViewModel.resetActionState()
            },
            onAccept = { myActivityViewModel.onAcceptOffer(requestToShowInDialog!!) },
            onDecline = { myActivityViewModel.onDeclineOffer(requestToShowInDialog!!.id) }
        )
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("My Activity", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Black
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                val (listToShow, emptyMessage) = when (page) {
                    0 -> myRequests to "You haven't posted any requests yet."
                    1 -> myResponses to "You haven't responded to any requests yet."
                    2 -> completedRequests to "You have no completed requests."
                    else -> emptyList<HelpRequest>() to ""
                }

                if (listToShow.isEmpty()) {
                    EmptyState(message = emptyMessage)
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(listToShow) { request ->
                            ActivityItemRow(
                                request = request,
                                onClick = {
                                    // Only show the dialog if the user is the owner and status is pending
                                    if (request.status == "pending" && request.userId == currentUser?.uid) {
                                        requestToShowInDialog = request
                                    } else {
                                        // Later, you can navigate to a simple read-only detail screen here if you want
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- CORRECTED ActivityItemRow ---
@Composable
fun OfferManagementDialog(
    request: HelpRequest,
    actionUiState: RequestUiState,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (actionUiState) {
                    is RequestUiState.Loading -> CircularProgressIndicator(color = Color.White)
                    is RequestUiState.Success -> Text("Success!", color = Color.Green, fontWeight = FontWeight.Bold)
                    is RequestUiState.Error -> Text(actionUiState.message, color = MaterialTheme.colorScheme.error)
                    is RequestUiState.Idle -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Offer from: ${request.responderName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = onAccept,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                                ) { Text("Accept", color = Color.Black) }
                                OutlinedButton(
                                    onClick = onDecline,
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, Color.Gray)
                                ) { Text("Decline", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    request: HelpRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Archive, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = formatTimestamp(request.createdAt), color = Color.Gray, fontSize = 12.sp)
            }
            Text(
                text = request.status.replaceFirstChar { it.uppercase() },
                color = getStatusColor(request.status),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- HELPER FUNCTIONS (Included for completeness) ---

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "completed" -> Color.Green
        "pending" -> Color.Yellow
        "open" -> Color.Gray
        else -> Color.LightGray
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val diff = Timestamp.now().seconds - timestamp.seconds
    val days = TimeUnit.SECONDS.toDays(diff)
    if (days > 0) return "Posted $days ${if (days == 1L) "day" else "days"} ago"
    val hours = TimeUnit.SECONDS.toHours(diff)
    if (hours > 0) return "Posted $hours ${if (hours == 1L) "hour" else "hours"} ago"
    val minutes = TimeUnit.SECONDS.toMinutes(diff)
    if (minutes > 0) return "Posted $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
    return "Posted just now"
}

@Preview
@Composable
fun MyActivityScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        MyActivityScreen()
    }
}
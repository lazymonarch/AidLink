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
import androidx.compose.material.icons.filled.*
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
import com.aidlink.viewmodel.MyActivityUiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyActivityScreen(
    myActivityViewModel: MyActivityViewModel,
    onNavigateToChat: (chatId: String, otherUserName: String) -> Unit
) {
    val tabTitles = listOf("My Requests", "My Responses", "Completed")
    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    val myRequests by myActivityViewModel.myRequests.collectAsState()
    val myResponses by myActivityViewModel.myResponses.collectAsState()
    val completedRequests by myActivityViewModel.completedRequests.collectAsState()

    var requestInDialog by remember { mutableStateOf<HelpRequest?>(null) }
    val actionUiState by myActivityViewModel.actionUiState.collectAsState()

    // --- NEW: State for the review dialog ---
    val requestToReview by myActivityViewModel.requestToReview.collectAsState()

    // Show the review dialog when the state is not null
    if (requestToReview != null) {
        ReviewDialog(
            helperName = requestToReview?.responderName ?: "the helper",
            onDismiss = { myActivityViewModel.dismissReviewDialog() },
            onSubmit = { rating, comment ->
                myActivityViewModel.submitReview(rating, comment)
            }
        )
    }
    // --- END NEW ---

    if (requestInDialog != null) {
        RequestManagementDialog(
            request = requestInDialog!!,
            actionUiState = actionUiState,
            onDismiss = {
                requestInDialog = null
                myActivityViewModel.resetActionState()
            },
            onAccept = { myActivityViewModel.onAcceptOffer(requestInDialog!!) },
            onDecline = { myActivityViewModel.onDeclineOffer(requestInDialog!!.id) },
            onDelete = { myActivityViewModel.onDeleteRequest(requestInDialog!!.id) },
            onCancel = { myActivityViewModel.onCancelRequest(requestInDialog!!.id) },
            onConfirm = { myActivityViewModel.onConfirmCompletion(requestInDialog!!) }
        )
    }

    LaunchedEffect(actionUiState) {
        when (val state = actionUiState) {
            is MyActivityUiState.NavigateToChat -> {
                onNavigateToChat(state.chatId, state.otherUserName)
                myActivityViewModel.resetActionState()
                requestInDialog = null
            }
            is MyActivityUiState.Success -> {
                delay(1500)
                requestInDialog = null
                myActivityViewModel.resetActionState()
            }
            else -> { /* Do nothing for other states */ }
        }
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
                    0 -> myRequests to "You haven't posted any requests."
                    1 -> myResponses to "You haven't responded to any requests."
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
                                    if (request.userId == currentUser?.uid && request.status != "completed") {
                                        requestInDialog = request
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

// --- NEW: The Review Dialog Composable ---
@Composable
fun ReviewDialog(
    helperName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How was your experience with $helperName?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                StarRatingBar(
                    rating = rating,
                    onRatingChanged = { newRating -> rating = newRating }
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Add a comment (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray,
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Skip")
                    }
                    Button(
                        onClick = { onSubmit(rating, comment) },
                        modifier = Modifier.weight(1f),
                        enabled = rating > 0
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun StarRatingBar(
    modifier: Modifier = Modifier,
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarOutline,
                contentDescription = "Star $i",
                tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onRatingChanged(i) }
                    .padding(4.dp)
            )
        }
    }
}
// --- END NEW ---

@Composable
fun RequestManagementDialog(
    request: HelpRequest,
    actionUiState: MyActivityUiState,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
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
                when (val state = actionUiState) {
                    is MyActivityUiState.Loading -> CircularProgressIndicator(color = Color.White)
                    is MyActivityUiState.Success -> Text("Success!", color = Color.Green, fontWeight = FontWeight.Bold)
                    is MyActivityUiState.NavigateToChat -> CircularProgressIndicator(color = Color.White)
                    is MyActivityUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    is MyActivityUiState.Idle -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = when (request.status) {
                                    "open" -> "Manage Your Request"
                                    "pending" -> "Offer from: ${request.responderName}"
                                    "in_progress" -> "Request in Progress"
                                    "pending_approval" -> "Confirm Completion"
                                    else -> "Request Details"
                                },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            when (request.status) {
                                "open" -> OpenRequestActions(onDelete = onDelete)
                                "pending" -> PendingRequestActions(onAccept = onAccept, onDecline = onDecline)
                                "in_progress" -> InProgressRequestActions(onCancel = onCancel)
                                "pending_approval" -> PendingApprovalActions(onConfirm = onConfirm)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun OpenRequestActions(onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { /* TODO: Navigate to Edit Screen */ }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit")
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red), border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Delete")
        }
    }
}

@Composable
fun PendingRequestActions(onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = onAccept, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Green)) { Text("Accept", color = Color.Black) }
        OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, Color.Gray)) { Text("Decline", color = Color.White) }
    }
}

@Composable
fun InProgressRequestActions(onCancel: () -> Unit) {
    Button(
        onClick = onCancel,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
    ) {
        Text("Cancel Request")
    }
}

@Composable
fun PendingApprovalActions(onConfirm: () -> Unit) {
    Text(
        text = "The helper has marked this job as complete. Please confirm to close the request.",
        color = Color.LightGray,
        textAlign = TextAlign.Center
    )
    Button(
        onClick = onConfirm,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Confirm Completion", color = Color.Black, fontWeight = FontWeight.Bold)
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
        "pending_approval" -> Color(0xFFFFA500) // Orange
        "in_progress" -> Color(0xFF4DABF7) // Blue
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
        MyActivityScreen(myActivityViewModel = viewModel(), onNavigateToChat = { _, _ -> })
    }
}
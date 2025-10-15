package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.aidlink.model.HelpRequest
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.MyActivityViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyActivityScreen(
    myActivityViewModel: MyActivityViewModel,
    onNavigateToChat: (chatId: String, otherUserName: String) -> Unit,
    navController: NavController
) {
    val tabTitles = listOf("My Requests", "My Responses", "Completed")
    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    val myRequests by myActivityViewModel.myRequests.collectAsState()
    val myResponses by myActivityViewModel.myResponses.collectAsState()
    val completedRequests by myActivityViewModel.completedRequests.collectAsState()

    var requestInDialog by remember { mutableStateOf<HelpRequest?>(null) }

    if (requestInDialog != null) {
        RequestManagementDialog(
            request = requestInDialog!!,
            onDismiss = { requestInDialog = null },
            onDelete = {
                myActivityViewModel.onDeleteRequest(requestInDialog!!.id)
                requestInDialog = null
            },
            onCancel = {
                myActivityViewModel.onCancelRequest(requestInDialog!!.id)
                requestInDialog = null
            },
            onConfirm = {
                myActivityViewModel.onConfirmCompletion(requestInDialog!!)
                requestInDialog = null
            },
            onNavigateToEdit = { requestId ->
                navController.navigate("edit_request/$requestId")
            }
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
                val listToShow = when (page) {
                    0 -> myRequests
                    1 -> myResponses
                    2 -> completedRequests
                    else -> emptyList()
                }

                val emptyMessage = when (page) {
                    0 -> "You haven't posted any requests."
                    1 -> "You haven't responded to any requests."
                    2 -> "You have no completed requests."
                    else -> ""
                }

                if (listToShow.isEmpty()) {
                    EmptyState(message = emptyMessage)
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(items = listToShow, key = { it.id }) { request ->
                            ActivityItemRow(
                                request = request,
                                currentUser = currentUser,
                                onClick = {
                                    if (request.userId == currentUser?.uid) {
                                        requestInDialog = request
                                    } else if (request.status in listOf("in_progress", "completed", "pending_completion")) {
                                        onNavigateToChat(request.id, request.userName)
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

@Composable
fun RequestManagementDialog(
    request: HelpRequest,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToEdit: (String) -> Unit // ✅ ADDED this parameter
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (request.status) {
                        "open" -> "Manage Your Request"
                        "in_progress" -> "Manage In-Progress Job"
                        "pending_completion" -> "Confirm Completion"
                        else -> "Request Details"
                    },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                when (request.status) {
                    // ✅ FIXED: Pass the navigation action to the buttons
                    "open" -> OpenRequestActions(
                        onDelete = onDelete,
                        onEdit = { onNavigateToEdit(request.id) }
                    )
                    "in_progress" -> InProgressRequestActions(onCancel = onCancel)
                    "pending_completion" -> PendingApprovalActions(onConfirm = onConfirm)
                }
            }
        }
    }
}

@Composable
fun ActivityItemRow(
    request: HelpRequest,
    currentUser: FirebaseUser?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ),
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
                Text(text = formatTimestamp(request.timestamp), color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                    color = getStatusColor(request.status),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (request.userId == currentUser?.uid && request.status == "open" && request.offerCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OfferCountBadge(count = request.offerCount)
                }
            }
        }
    }
}

@Composable
fun OpenRequestActions(onDelete: () -> Unit, onEdit: () -> Unit) { // ✅ ADDED onEdit parameter
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // ✅ FIXED: The onClick is now wired to the onEdit function
        Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
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
fun InProgressRequestActions(onCancel: () -> Unit) {
    Button(
        onClick = onCancel,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
    ) {
        Text("Cancel Request")
    }
}

@Composable
fun OfferCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
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
        "pending_completion" -> Color(0xFFFFA500)
        "in_progress" -> Color(0xFF4DABF7)
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
        // ✅ FIXED: Pass a NavController for the preview to build successfully
        MyActivityScreen(
            myActivityViewModel = viewModel(),
            onNavigateToChat = { _, _ -> },
            navController = rememberNavController()
        )
    }
}
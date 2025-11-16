
package com.aidlink.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aidlink.model.HelpRequest
import com.aidlink.viewmodel.MyActivityViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyActivityScreen(
    myActivityViewModel: MyActivityViewModel,
    onNavigateToChat: (chatId: String, otherUserName: String) -> Unit,
    navController: NavController
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val currentUser = Firebase.auth.currentUser

    val myRequests by myActivityViewModel.myRequests.collectAsState()
    val myResponses by myActivityViewModel.myResponses.collectAsState()
    val completedRequests by myActivityViewModel.completedRequests.collectAsState()

    var managingRequest by remember { mutableStateOf<HelpRequest?>(null) }

    if (managingRequest != null) {
        ManageRequestDialog(
            request = managingRequest!!,
            onDismiss = { managingRequest = null },
            onAcceptOffer = { offer ->
                myActivityViewModel.onAcceptOffer(managingRequest!!.id, offer.helperId)
                managingRequest = null
            },
            onDelete = {
                myActivityViewModel.onDeleteRequest(managingRequest!!.id)
                managingRequest = null
            },
            onEdit = {
                val req = managingRequest
                managingRequest = null
                navController.navigate("edit_request/${req!!.id}")
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "My Activity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(
                                    currentTabPosition = tabPositions[pagerState.currentPage]
                                ),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    divider = {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            TabWithBadge(
                                title = "My Requests",
                                count = myRequests.size,
                                isSelected = pagerState.currentPage == 0
                            )
                        }
                    )

                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            TabWithBadge(
                                title = "My Responses",
                                count = myResponses.size,
                                isSelected = pagerState.currentPage == 1
                            )
                        }
                    )

                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = {
                            TabWithBadge(
                                title = "Completed",
                                count = completedRequests.size,
                                isSelected = pagerState.currentPage == 2
                            )
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> MyRequestsContent(
                    requests = myRequests,
                    onRequestClick = { managingRequest = it },
                    onPostRequest = { navController.navigate("post_request") }
                )
                1 -> MyResponsesContent(
                    responses = myResponses,
                    onResponseClick = { request ->
                        if (request.status == "in_progress") {
                            onNavigateToChat(request.id, request.userName)
                        } else {
                            navController.navigate("request_detail/${request.id}")
                        }
                    }
                )
                2 -> CompletedContent(
                    completedRequests = completedRequests,
                    currentUser = currentUser,
                    onRequestClick = { /* Show summary */ },
                    onLeaveFeedback = { request ->
                        val isCurrentUserTheRequester = request.userId == currentUser?.uid
                        val revieweeId = if (isCurrentUserTheRequester)
                            request.responderId ?: ""
                        else
                            request.userId
                        if (revieweeId.isNotEmpty()) {
                            navController.navigate("review/${request.id}/$revieweeId/${!isCurrentUserTheRequester}")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TabWithBadge(
    title: String,
    count: Int,
    isSelected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        if (count > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun MyRequestsContent(
    requests: List<HelpRequest>,
    onRequestClick: (HelpRequest) -> Unit,
    onPostRequest: () -> Unit
) {
    if (requests.isEmpty()) {
        EmptyStateWithAction(
            icon = Icons.AutoMirrored.Filled.Assignment,
            title = "No Requests Yet",
            message = "You haven't posted any requests.\nStart by posting your first help request!",
            actionText = "Post a Request",
            onAction = onPostRequest
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = requests, key = { it.id }) { request ->
                ActivityRequestCard(
                    request = request,
                    onClick = { onRequestClick(request) }
                )
            }
        }
    }
}

@Composable
internal fun MyResponsesContent(
    responses: List<HelpRequest>,
    onResponseClick: (HelpRequest) -> Unit
) {
    if (responses.isEmpty()) {
        EmptyStateWithAction(
            icon = Icons.Default.Handshake,
            title = "No Responses Yet",
            message = "You haven't offered help to anyone yet.\nBrowse requests and start helping!",
            actionText = "Browse Requests",
            onAction = { /* Navigate to home */ }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = responses, key = { it.id }) { response ->
                ResponseCard(
                    request = response,
                    onClick = { onResponseClick(response) }
                )
            }
        }
    }
}

@Composable
internal fun CompletedContent(
    completedRequests: List<HelpRequest>,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    onRequestClick: (HelpRequest) -> Unit,
    onLeaveFeedback: (HelpRequest) -> Unit
) {
    if (completedRequests.isEmpty()) {
        EmptyStateWithoutAction(
            icon = Icons.Default.CheckCircle,
            title = "No Completed Requests",
            message = "Completed requests will appear here.\nKeep helping and building your reputation!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = completedRequests, key = { it.id }) { request ->
                Column {
                    CompletedRequestCard(
                        request = request,
                        currentUser = currentUser,
                        onClick = { onRequestClick(request) }
                    )

                    if (request.reviewStatus[currentUser?.uid] == "pending") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onLeaveFeedback(request) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.RateReview,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Leave Feedback",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyStateWithAction(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAction,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 3.dp
            )
        ) {
            Icon(
                imageVector = if (actionText.contains("Post"))
                    Icons.Default.Add
                else
                    Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                actionText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun EmptyStateWithoutAction(
    icon: ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

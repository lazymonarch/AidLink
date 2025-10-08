package com.aidlink.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.MyActivityViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyActivityScreen(
    myActivityViewModel: MyActivityViewModel = viewModel()
) {
    val tabTitles = listOf("My Requests", "My Responses", "Completed")
    val pagerState = rememberPagerState { tabTitles.size }
    val coroutineScope = rememberCoroutineScope()

    // Collect the live data from the ViewModel
    val myRequests by myActivityViewModel.myRequests.collectAsState()
    val myResponses by myActivityViewModel.myResponses.collectAsState()
    val completedRequests by myActivityViewModel.completedRequests.collectAsState()

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

                if (listToShow.isEmpty()) {
                    EmptyState(message = "You have no activity in this section yet.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        items(listToShow) { request ->
                            // You can customize this row further
                            ActivityItemRow(
                                icon = Icons.Default.Archive,
                                title = request.title,
                                subtitle = "Posted...", // You'll need to add a date to your model for this
                                status = request.status.replaceFirstChar { it.uppercase() },
                                statusColor = if(request.status == "completed") Color.Green else Color.Gray
                            )
                        }
                    }
                }
            }
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

@Composable
fun ActivityItemRow(icon: ImageVector, title: String, subtitle: String, status: String, statusColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Text(text = status, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
fun MyActivityScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        MyActivityScreen()
    }
}
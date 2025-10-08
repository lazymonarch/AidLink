package com.aidlink.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onPostRequestClicked: () -> Unit
) {
    // This is the key line: It gets the live list of requests from the ViewModel.
    val requests by homeViewModel.requests.collectAsState()

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("AidLink", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* TODO: Handle notifications */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post a request")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // The LazyColumn now displays the live data from the ViewModel
            items(requests) { request ->
                HelpRequestCard(request = request)
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        // We pass a dummy ViewModel for the preview to work
        HomeScreen(homeViewModel = viewModel(), onPostRequestClicked = {})
    }
}
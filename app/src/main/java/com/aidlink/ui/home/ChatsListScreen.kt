package com.aidlink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.Chat
import com.aidlink.ui.theme.AidLinkTheme
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider

// You'll need to add some placeholder avatar drawables to your res/drawable folder
// For example, create R.drawable.avatar_1, R.drawable.avatar_2, etc.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen() {
    // Dummy data for the chat list
    val chats = listOf(
        Chat("1", "Liam Carter", "Sure, I can help with that!", "10:42 AM", 2, true, "avatar_1"),
        Chat("2", "Sophia Bennett", "I'm available tomorrow afternoon.", "Yesterday", 0, false, "avatar_2"),
        Chat("3", "Ethan Walker", "Thanks for offering your help!", "Mon", 0, false, "avatar_3"),
        Chat("4", "Olivia Hayes", "No problem at all, happy to assist.", "Sun", 0, true, "avatar_4"),
        Chat("5", "Noah Foster", "Let me know if you need anything else.", "10/15/23", 0, false, "avatar_5")
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open navigation drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: New chat */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(chats) { chat ->
                ChatItemRow(chat = chat)
                HorizontalDivider(Modifier, DividerDefaults.Thickness, color = Color.DarkGray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItemRow(chat: Chat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            // In a real app, you would use an image loading library like Coil
            // For now, we use a placeholder color
            Box(modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.Gray)
            )
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .border(1.dp, Color.Black, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.userName, color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                text = chat.lastMessage,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = chat.timestamp, color = Color.Gray, fontSize = 12.sp)
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(text = chat.unreadCount.toString())
                }
            }
        }
    }
}

@Preview
@Composable
fun ChatsListScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        ChatsListScreen()
    }
}
package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.model.ChatWithStatus
import com.aidlink.viewmodel.ChatViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    chatViewModel: ChatViewModel,
    onChatClicked: (chatId: String, userName: String) -> Unit
) {
    val chatsWithStatus by chatViewModel.chats.collectAsState()
    val currentUser = Firebase.auth.currentUser
    val isInSelectionMode by chatViewModel.isInSelectionMode.collectAsState()
    val selectedChatIds by chatViewModel.selectedChatIds.collectAsState()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isInSelectionMode) {
                        IconButton(onClick = { chatViewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Selection Mode")
                        }
                    }
                },
                actions = {
                    if (isInSelectionMode) {
                        IconButton(
                            onClick = { chatViewModel.deleteSelectedChats() },
                            enabled = selectedChatIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected Chats")
                        }
                    } else {
                        IconButton(onClick = { chatViewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Select Chats")
                        }
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
            contentPadding = PaddingValues(16.dp)
        ) {
            items(chatsWithStatus, key = { it.chat.id }) { chatWithStatus ->
                val otherUserId = chatWithStatus.chat.participants.firstOrNull { it != currentUser?.uid } ?: ""
                val otherUserName = chatWithStatus.chat.participantInfo[otherUserId]?.get("name") ?: "Unknown User"
                val isSelected = selectedChatIds.contains(chatWithStatus.chat.id)
                val isDeletable = chatWithStatus.requestStatus != "in_progress"

                ChatItemRow(
                    chatWithStatus = chatWithStatus,
                    otherUserName = otherUserName,
                    isInSelectionMode = isInSelectionMode,
                    isSelected = isSelected,
                    isDeletable = isDeletable,
                    onItemClick = {
                        if (isInSelectionMode) {
                            if(isDeletable) {
                                chatViewModel.toggleChatSelection(chatWithStatus.chat.id)
                            }
                        } else {
                            onChatClicked(chatWithStatus.chat.id, otherUserName)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItemRow(
    chatWithStatus: ChatWithStatus,
    otherUserName: String,
    isInSelectionMode: Boolean,
    isSelected: Boolean,
    isDeletable: Boolean,
    onItemClick: () -> Unit
) {
    val borderColor = when {
        !isDeletable -> Color.Green.copy(alpha = 0.8f) // In Progress
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    enabled = isDeletable
                )
            } else {
                Box {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = otherUserName, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = chatWithStatus.chat.lastMessage,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                val formattedTimestamp = chatWithStatus.chat.lastMessageTimestamp?.toDate()?.let {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                } ?: ""
                Text(text = formattedTimestamp, color = Color.Gray, fontSize = 12.sp)
                if (chatWithStatus.chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(text = chatWithStatus.chat.unreadCount.toString())
                    }
                }
            }
        }
    }
}

package com.aidlink.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.model.Chat
import com.aidlink.viewmodel.ChatViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import com.aidlink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    chatViewModel: ChatViewModel,
    onChatClicked: (chatId: String, userName: String) -> Unit
) {
    val chats by chatViewModel.chats.collectAsState()
    val currentUser = Firebase.auth.currentUser
    var isInDeleteMode by remember { mutableStateOf(false) }
    var selectedChats by remember { mutableStateOf(setOf<String>()) }
    val hasCompletedChats = chats.any { it.status == "completed" }
    LaunchedEffect(hasCompletedChats) {
        if (!hasCompletedChats) {
            isInDeleteMode = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold) },
                actions = {
                    if (hasCompletedChats) {
                        IconButton(onClick = { isInDeleteMode = !isInDeleteMode }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Select Chats to Delete"
                            )
                        }
                    }
                }
            )
        },

        bottomBar = {
            AnimatedVisibility(
                visible = isInDeleteMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomAppBar {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            selectedChats.forEach { chatId ->
                                chatViewModel.deleteChat(chatId)
                            }
                            selectedChats = setOf()
                            isInDeleteMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = selectedChats.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete (${selectedChats.size})")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(chats, key = { it.id }) { chat ->
                val otherUserId = chat.participants.firstOrNull { it != currentUser?.uid } ?: ""
                val otherUserInfo = chat.participantInfo[otherUserId]
                val otherUserName = otherUserInfo?.get("name") ?: "Unknown User"
                val otherUserPhotoUrl = otherUserInfo?.get("photoUrl") ?: ""

                ChatItemRow(
                    chat = chat,
                    otherUserName = otherUserName,
                    otherUserPhotoUrl = otherUserPhotoUrl,
                    isInDeleteMode = isInDeleteMode,
                    isSelected = chat.id in selectedChats,
                    onItemClick = {
                        if (isInDeleteMode) {
                            if (chat.status == "completed") {
                                selectedChats = if (chat.id in selectedChats) {
                                    selectedChats - chat.id
                                } else {
                                    selectedChats + chat.id
                                }
                            }
                        } else {
                            onChatClicked(chat.id, otherUserName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    otherUserName: String,
    otherUserPhotoUrl: String,
    isInDeleteMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val borderColor = when (chat.status) {
        "in_progress" -> MaterialTheme.colorScheme.primary
        "pending_completion" -> MaterialTheme.colorScheme.tertiary
        "completed" -> MaterialTheme.colorScheme.error
        else -> Color.Transparent
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onItemClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isInDeleteMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    enabled = chat.status == "completed"
                )
            }

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(otherUserPhotoUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .build(),
                contentDescription = "Profile Picture of $otherUserName",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = otherUserName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = chat.lastMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                val formattedTimestamp = chat.lastMessageTimestamp?.toDate()?.let { date ->
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
                } ?: ""
                Text(text = formattedTimestamp, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

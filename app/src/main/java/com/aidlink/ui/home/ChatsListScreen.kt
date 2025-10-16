package com.aidlink.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.model.Chat
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
        containerColor = Color.Black,
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },

        bottomBar = {
            AnimatedVisibility(
                visible = isInDeleteMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomAppBar(
                    containerColor = Color.Black
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            selectedChats.forEach { chatId ->
                                chatViewModel.deleteChat(chatId)
                            }
                            selectedChats = setOf()
                            isInDeleteMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
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
            contentPadding = PaddingValues(16.dp)
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
        "in_progress" -> Color.Green
        "pending_completion" -> Color(0xFFFFC107)
        "completed" -> Color.Red
        else -> Color.Transparent
    }

    val backgroundColor = if (isSelected) {
        Color(0xFF3A3A3C)
    } else {
        Color(0xFF1C1C1E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
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
                enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                exit =fadeOut(animationSpec = tween(durationMillis = 200))
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
                    .build(),
                contentDescription = "Profile Picture of $otherUserName",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = otherUserName, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = chat.lastMessage,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                val formattedTimestamp = chat.lastMessageTimestamp?.toDate()?.let {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
                } ?: ""
                Text(text = formattedTimestamp, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
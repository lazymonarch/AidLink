package com.aidlink.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.model.Message
import com.aidlink.model.MessageType
import com.aidlink.model.shouldShowAvatar
import com.aidlink.model.shouldShowTimestamp
import com.aidlink.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    userName: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val otherUserTyping by viewModel.otherUserTyping.collectAsState()
    val currentRequest by viewModel.currentRequest.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            ChatTopBar(
                userName = userName,
                isTyping = otherUserTyping,
                onBackClick = { navController.navigateUp() },
                onProfileClick = { /* TODO */ },
                onMoreClick = { /* TODO */ }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            currentRequest?.let { request ->
                RequestInfoCard(
                    request = request,
                    onClick = { navController.navigate("request_detail/${request.id}") }
                )
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                        val prevMessage = messages.getOrNull(index - 1)
                        val nextMessage = messages.getOrNull(index + 1)
                        when (message.type) {
                            MessageType.USER -> UserMessage(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                showAvatar = message.shouldShowAvatar(prevMessage),
                                showTimestamp = message.shouldShowTimestamp(nextMessage)
                            )
                            MessageType.SYSTEM -> SystemMessage(message = message)
                            else -> {}
                        }
                    }
                    
                    if (otherUserTyping) {
                        item { TypingIndicator() }
                    }
                }
                
                ScrollToBottomButton(
                    listState = listState,
                    messageCount = messages.size,
                    onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } }
                )
            }
            
            MessageInputBar(
                messageText = messageText,
                isSending = isSending,
                onMessageChange = viewModel::updateMessageText,
                onSendClick = viewModel::sendMessage,
                onAttachmentClick = { /* TODO */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    userName: String,
    isTyping: Boolean,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onProfileClick)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    AnimatedContent(
                        targetState = isTyping,
                        transitionSpec = { fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically() },
                        label = "typing_status"
                    ) { typing ->
                        Text(
                            text = if (typing) "typing..." else "Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (typing) Color(0xFFFF6B35) else Color(0xFF43A047),
                            fontStyle = if (typing) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(onClick = onMoreClick) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
private fun RequestInfoCard(
    request: com.aidlink.model.HelpRequest,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = Color(0xFFFF6B35), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = request.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "View request details", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View request", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SystemMessage(message: Message) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = getSystemMessageIcon(message.systemType), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                    Text(text = message.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
                if (message.requiresAction && !message.actionCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text(text = "Confirm", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessage(
    message: Message,
    isOwnMessage: Boolean,
    showAvatar: Boolean,
    showTimestamp: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f).then(if (isOwnMessage) Modifier.padding(start = 48.dp) else Modifier.padding(end = 48.dp)),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isOwnMessage) {
                if (showAvatar) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Text(text = message.senderName?.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isOwnMessage) 20.dp else 4.dp,
                    topEnd = if (isOwnMessage) 4.dp else 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isOwnMessage) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = if (isOwnMessage) 0.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.onSurface)
                    Row(modifier = Modifier.align(Alignment.End).padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = formatMessageTime(message.timestamp), style = MaterialTheme.typography.labelSmall, color = if (isOwnMessage) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        if (isOwnMessage) {
                            Icon(
                                imageVector = when {
                                    message.isRead -> Icons.Default.DoneAll
                                    message.isDelivered -> Icons.Default.Done
                                    message.isSent -> Icons.Default.Done
                                    else -> Icons.Default.Schedule
                                },
                                contentDescription = getMessageStatusDescription(message),
                                tint = if (message.isRead) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
        if (showTimestamp) {
            Text(text = formatMessageTimestamp(message.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = if (isOwnMessage) 0.dp else 40.dp, end = if (isOwnMessage) 8.dp else 0.dp, top = 4.dp))
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.Start) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 1.dp) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing_anim")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse, initialStartOffset = StartOffset(index * 200)),
                        label = "typing_dot_alpha"
                    )
                    Box(modifier = Modifier.size(8.dp).alpha(alpha).background(color = Color(0xFFFF6B35), shape = CircleShape))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ScrollToBottomButton(
    listState: androidx.compose.foundation.lazy.LazyListState,
    messageCount: Int,
    onClick: () -> Unit
) {
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 5 && messageCount > 0 } }
    AnimatedVisibility(visible = showButton, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(), modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = Color(0xFFFF6B35),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
        }
    }
}

@Composable
private fun MessageInputBar(
    messageText: String,
    isSending: Boolean,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachmentClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onAttachmentClick, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add attachment", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 120.dp),
                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    cursorColor = Color(0xFFFF6B35)
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier.size(48.dp),
                containerColor = if (messageText.isNotBlank() && !isSending) Color(0xFFFF6B35) else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (messageText.isNotBlank() && !isSending) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun getSystemMessageIcon(systemType: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (systemType) {
        "offer_accepted" -> Icons.Default.CheckCircle
        "job_completed" -> Icons.Default.TaskAlt
        "chat_archived" -> Icons.Default.Archive
        else -> Icons.Default.Info
    }
}

private fun getMessageStatusDescription(message: Message): String {
    return when {
        message.isRead -> "Read"
        message.isDelivered -> "Delivered"
        message.isSent -> "Sent"
        else -> "Sending"
    }
}

private fun formatMessageTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
}

private fun formatMessageTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = timestamp.toDate() }
    return when {
        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
        }
        now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 && now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            "Yesterday ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())}"
        }
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(timestamp.toDate())
        }
        else -> {
            SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(timestamp.toDate())
        }
    }
}
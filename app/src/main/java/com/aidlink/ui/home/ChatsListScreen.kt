package com.aidlink.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aidlink.model.Chat
import com.aidlink.viewmodel.ChatsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatsListScreen - Material 3 v1.3.0 Implementation
 * Features: Pull-to-refresh, search, selection mode, empty states
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsListScreen(
    navController: NavController,
    viewModel: ChatsViewModel = hiltViewModel()
) {
    // State collection
    val chats by viewModel.chats.collectAsState()
    val selectedChats by viewModel.selectedChats.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val showSearch by viewModel.showSearch.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                // Selection mode top bar
                TopAppBar(
                    title = {
                        Text(
                            "${selectedChats.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.selectAll() },
                            enabled = selectedChats.size < chats.size
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select all chats",
                                tint = if (selectedChats.size < chats.size)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFFFE5DD) // Light orange for selection
                    )
                )
            } else {
                // Normal top bar
                ChatsTopBar(
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    onSearchToggle = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearchClear = { viewModel.clearSearch() },
                    onNewChatClick = { /* Navigate to new chat */ }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Delete FAB in selection mode
            if (selectionMode && selectedChats.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.deleteSelectedChats() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            "Delete (${selectedChats.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        // Empty state
        if (chats.isEmpty() && !isRefreshing) {
            ChatsEmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            // Chat list with pull-to-refresh (Material 3 v1.3.0)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshChats() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = chats,
                        key = { it.id }
                    ) { chat ->
                        ChatListItem(
                            chat = chat,
                            isSelected = selectedChats.contains(chat.id),
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleChatSelection(chat.id)
                                } else {
                                    navController.navigate("chat/${chat.id}/${chat.otherUserName}")
                                }
                            },
                            onLongClick = {
                                viewModel.enterSelectionMode(chat.id)
                            },
                            // Use animateItem() instead of animateItemPlacement()
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top bar with search functionality
 */
@Composable
private fun ChatsTopBar(
    showSearch: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Search button
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search chats",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // New chat button
                IconButton(onClick = onNewChatClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Start new chat",
                        tint = Color(0xFFFF6B35)
                    )
                }
            }
        }

        // Search bar
        AnimatedVisibility(
            visible = showSearch,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search conversations...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = onSearchClear) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF6B35),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    cursorColor = Color(0xFFFF6B35)
                ),
                singleLine = true
            )
        }
    }
}

/**
 * Empty state for chats list
 */
@Composable
private fun ChatsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Conversations Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start helping others or request help\nto begin chatting!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * Individual chat list item with selection support
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: Chat,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected)
            Color(0xFFFFE5DD).copy(alpha = 0.5f)
        else
            Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            AnimatedVisibility(
                visible = selectionMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFFFF6B35),
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            // User avatar with badges
            UserAvatar(
                photoUrl = chat.otherUserPhotoUrl,
                unreadCount = chat.unreadCount,
                isOnline = chat.isOnline
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Chat info
            ChatInfo(
                chat = chat,
                modifier = Modifier.weight(1f)
            )

            // Pinned indicator
            if (chat.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(-45f)
                )
            }
        }
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(start = 84.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * User avatar with unread badge and online indicator
 */
@Composable
private fun UserAvatar(
    photoUrl: String?,
    unreadCount: Int,
    isOnline: Boolean
) {
    Box {
        AsyncImage(
            model = photoUrl,
            contentDescription = "User avatar",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        // Unread badge
        if (unreadCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                containerColor = Color(0xFFFF6B35),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else "$unreadCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF43A047), CircleShape)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    )
            )
        }
    }
}

/**
 * Chat information (name, message preview, timestamp)
 */
@Composable
private fun ChatInfo(
    chat: Chat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(end = 8.dp)
    ) {
        // Name and timestamp row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chat.otherUserName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (chat.unreadCount > 0)
                    FontWeight.Bold
                else
                    FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatChatTimestamp(chat.lastMessageTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = if (chat.unreadCount > 0)
                    Color(0xFFFF6B35)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (chat.unreadCount > 0)
                    FontWeight.Bold
                else
                    FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Message preview or typing indicator
        if (chat.isTyping) {
            TypingIndicator()
        } else {
            MessagePreview(chat = chat)
        }
    }
}

/**
 * Typing indicator animation
 */
@Composable
private fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot_alpha"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(
                        color = Color(0xFFFF6B35),
                        shape = CircleShape
                    )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "typing...",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF6B35),
            fontStyle = FontStyle.Italic
        )
    }
}

/**
 * Message preview with status icon
 */
@Composable
private fun MessagePreview(chat: Chat) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Message status icon (if sent by me)
        if (chat.lastMessageSentByMe) {
            Icon(
                imageVector = when {
                    chat.lastMessageRead -> Icons.Default.DoneAll
                    chat.lastMessageDelivered -> Icons.Default.Done
                    else -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = when {
                    chat.lastMessageRead -> Color(0xFF1E88E5)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = chat.lastMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (chat.unreadCount > 0)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (chat.unreadCount > 0)
                FontWeight.Medium
            else
                FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Format timestamp for chat list
 */
fun formatChatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""

    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply {
        time = timestamp.toDate()
    }

    return when {
        // Today - show time
        now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
        }
        // Yesterday
        now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            "Yesterday"
        }
        // This week - show day name
        now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) &&
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(timestamp.toDate())
        }
        // Older - show date
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
        }
    }
}
package com.aidlink.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, String>> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Int = 0,
    val status: String = "",
    val deletedBy: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val lastMessageSentByMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val lastMessageDelivered: Boolean = false,
    val isPinned: Boolean = false,
    val otherUserName: String = "",
    val otherUserPhotoUrl: String = ""
)
package com.aidlink.model

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, String>> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Int = 0
)
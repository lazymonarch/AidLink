///Users/lakshan/AndroidStudioProjects/AidLink/app/src/main/java/com/aidlink/model/Chat.kt

package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Chat(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantInfo: Map<String, Map<String, String>> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    @get:Exclude val unreadCount: Int = 0,
    @get:Exclude val status: String = "",
    val deletedBy: List<String> = emptyList(),
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val lastMessageSentByMe: Boolean = false,
    val lastMessageRead: Boolean = false,
    val lastMessageDelivered: Boolean = false,
    val isPinned: Boolean = false,
    val otherUserName: String = "",
    val otherUserPhotoUrl: String = "",

    // NEW FIELDS for job completion
    val requestId: String = "",
    val requestStatus: String = "in_progress",
    val helperId: String = "",
    val requesterId: String = ""
)
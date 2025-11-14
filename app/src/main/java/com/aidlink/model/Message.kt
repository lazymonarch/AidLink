package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Enhanced Message model with delivery status and typing
 * Supports Material 3 v1.3.0 chat features
 */
data class Message(
    @DocumentId
    val id: String = "",
    
    // Basic message data
    val senderId: String = "",
    val senderName: String? = null,
    val senderPhotoUrl: String? = null,
    val text: String = "",
    val timestamp: Timestamp? = null,
    
    // Message type
    val type: MessageType = MessageType.USER,
    
    // Delivery status (for own messages)
    val isSent: Boolean = false,           // Successfully sent to server
    val isDelivered: Boolean = false,      // Delivered to recipient's device
    val isRead: Boolean = false,           // Read by recipient
    val isFailed: Boolean = false,         // Failed to send
    
    // System message properties
    val systemType: String? = null,        // "offer_accepted", "job_completed", etc.
    val requiresAction: Boolean = false,   // Does this message need user action?
    val actionCompleted: Boolean = false,  // Has the action been completed?
    
    // Media attachments (future feature)
    val hasImage: Boolean = false,
    val imageUrl: String? = null,
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    // Reply/thread support (future feature)
    val replyToId: String? = null,
    val replyToText: String? = null,
    
    // Editing
    val isEdited: Boolean = false,
    val editedAt: Timestamp? = null
)

/**
 * Message type enum
 */
enum class MessageType {
    USER,      // Regular user message
    SYSTEM,    // System-generated message
    IMAGE,     // Image message (future)
    LOCATION   // Location message (future)
}

/**
 * System message types
 */
object SystemMessageType {
    const val OFFER_ACCEPTED = "offer_accepted"
    const val JOB_STARTED = "job_started"
    const val JOB_COMPLETED = "job_completed"
    const val JOB_CONFIRMED = "job_confirmed"
    const val CHAT_ARCHIVED = "chat_archived"
    const val REQUEST_CANCELLED = "request_cancelled"
}

/**
 * Message status for UI display
 */
sealed class MessageStatus {
    object Sending : MessageStatus()
    object Sent : MessageStatus()
    object Delivered : MessageStatus()
    object Read : MessageStatus()
    object Failed : MessageStatus()
}

/**
 * Extension function to get message status
 */
fun Message.getStatus(): MessageStatus {
    return when {
        isFailed -> MessageStatus.Failed
        isRead -> MessageStatus.Read
        isDelivered -> MessageStatus.Delivered
        isSent -> MessageStatus.Sent
        else -> MessageStatus.Sending
    }
}

/**
 * Extension function to check if message is from current user
 */
fun Message.isFromUser(userId: String?): Boolean {
    return senderId == userId
}

/**
 * Extension function to check if message should show timestamp
 */
fun Message.shouldShowTimestamp(nextMessage: Message?): Boolean {
    if (nextMessage == null) return true
    
    val currentTime = timestamp?.seconds ?: return false
    val nextTime = nextMessage.timestamp?.seconds ?: return false
    
    return (nextTime - currentTime) > 300 // 5 minutes
}

/**
 * Extension function to check if message should show avatar
 */
fun Message.shouldShowAvatar(previousMessage: Message?): Boolean {
    if (previousMessage == null) return true
    return previousMessage.senderId != this.senderId
}
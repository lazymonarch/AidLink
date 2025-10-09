package com.aidlink.model

import com.google.firebase.Timestamp

enum class RequestType {
    VOLUNTEER, FEE
}

data class HelpRequest(
    val id: String,
    val userId: String = "", // <-- ADD THIS LINE
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val type: RequestType,
    val status: String,
    val createdAt: Timestamp? = null,
    val responderId: String? = null,
    val responderName: String? = null
)
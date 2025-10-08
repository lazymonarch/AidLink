package com.aidlink.model

// This enum is unchanged
enum class RequestType {
    VOLUNTEER, FEE
}

// Add the 'status' field to your data class
data class HelpRequest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val type: RequestType,
    val status: String // <-- ADD THIS LINE
)
package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

enum class RequestType {
    VOLUNTEER, FEE
}

data class HelpRequest(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val type: RequestType = RequestType.FEE,
    val status: String = "open",

    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val responderId: String? = null,
    val responderName: String? = null,

    // --- Fields for backend actions ---
    val action: String? = null,
    @ServerTimestamp
    val lastActionTimestamp: Timestamp? = null
)
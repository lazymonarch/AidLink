package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

enum class RequestType {
    VOLUNTEER, FEE
}

data class HelpRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val compensation: String = "",
    val type: RequestType = RequestType.FEE,
    val status: String = "open",
    val offerCount: Int = 0,

    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val responderId: String? = null,
    val responderName: String? = null,

    val action: String? = null,
    @ServerTimestamp
    val lastActionTimestamp: Timestamp? = null
)
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
    val locationName: String = "",
    val compensation: String = "",
    val type: RequestType = RequestType.FEE,
    val status: String = "open",
    val offerCount: Int = 0,
    val reviewStatus: Map<String, String> = emptyMap(),

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geohash: String = "",
    val geohashCoarse: String = "",

    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val responderId: String? = null,
    val responderName: String? = null,
)
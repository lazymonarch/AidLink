package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Offer(
    val id: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val status: String = "pending",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
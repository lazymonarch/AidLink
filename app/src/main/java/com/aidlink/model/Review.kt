package com.aidlink.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review(
    val requestId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val overall: String = "",
    val badges: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Date? = null
)
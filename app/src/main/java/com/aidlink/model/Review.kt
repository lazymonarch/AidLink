package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Review(
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
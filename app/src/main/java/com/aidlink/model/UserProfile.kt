package com.aidlink.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val area: String = "",
    val location: GeoPoint? = null,
    val fcmToken: String = "",
    val phone: String = "",
    val helpsCompleted: Int = 0,
    val requestsPosted: Int = 0,
    val trustBadges: Map<String, Int> = emptyMap(),
    @ServerTimestamp val createdAt: Date? = null
)
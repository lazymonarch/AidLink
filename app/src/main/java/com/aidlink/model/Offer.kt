///Users/lakshan/AndroidStudioProjects/AidLink/app/src/main/java/com/aidlink/model/Offer.kt

package com.aidlink.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Offer(
    val id: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val helperPhotoUrl: String = "",
    val status: String = "pending",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
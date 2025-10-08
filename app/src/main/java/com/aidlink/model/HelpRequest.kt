// File: com/aidlink/model/HelpRequest.kt
package com.aidlink.model

enum class RequestType {
    VOLUNTEER, FEE
}

data class HelpRequest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val type: RequestType
)
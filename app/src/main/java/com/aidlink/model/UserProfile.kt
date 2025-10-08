package com.aidlink.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val bio: String = "",
    val area: String = "",
    val phone: String = "",
    val skills: List<String> = emptyList()
)
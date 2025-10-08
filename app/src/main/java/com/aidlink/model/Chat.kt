package com.aidlink.model

data class Chat(
    val id: String,
    val userName: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val avatarUrl: String
)
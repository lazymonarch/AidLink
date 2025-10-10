package com.aidlink.viewmodel

// This single, shared sealed class will be used by all ViewModels that perform actions.
sealed class RequestUiState {
    object Idle : RequestUiState()
    object Loading : RequestUiState()
    object Success : RequestUiState()
    data class NavigateToChat(val chatId: String, val otherUserName: String) : RequestUiState()
    data class Error(val message: String) : RequestUiState()
}
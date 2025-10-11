package com.aidlink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.ChatWithStatus
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import com.aidlink.utils.authStateFlow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val _chats = MutableStateFlow<List<ChatWithStatus>>(emptyList())
    val chats: StateFlow<List<ChatWithStatus>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isInSelectionMode = MutableStateFlow(false)
    val isInSelectionMode: StateFlow<Boolean> = _isInSelectionMode.asStateFlow()

    private val _selectedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedChatIds: StateFlow<Set<String>> = _selectedChatIds.asStateFlow()

    private val _currentRequest = MutableStateFlow<HelpRequest?>(null)
    val currentRequest: StateFlow<HelpRequest?> = _currentRequest.asStateFlow()

    init {
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collect { user ->
                if (user != null) {
                    fetchChats(user.uid)
                } else {
                    _chats.value = emptyList()
                }
            }
        }
    }

    private fun fetchChats(userId: String) {
        viewModelScope.launch {
            repository.getChats(userId).collect { chatList ->
                _chats.value = chatList
            }
        }
    }

    fun fetchMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        val senderId = Firebase.auth.currentUser?.uid
        if (senderId == null || text.isBlank()) {
            return
        }
        val message = Message(senderId = senderId, text = text)
        viewModelScope.launch {
            repository.sendMessage(chatId, message)
        }
    }

    fun toggleSelectionMode() {
        _isInSelectionMode.update { !it }
        if (!_isInSelectionMode.value) {
            _selectedChatIds.value = emptySet()
        }
    }

    fun toggleChatSelection(chatId: String) {
        _selectedChatIds.update { currentIds ->
            if (currentIds.contains(chatId)) {
                currentIds - chatId
            } else {
                currentIds + chatId
            }
        }
    }

    fun deleteSelectedChats() {
        viewModelScope.launch {
            val idsToDelete = _selectedChatIds.value
            if (idsToDelete.isEmpty()) return@launch

            val success = repository.deleteChats(idsToDelete)
            if (success) {
                toggleSelectionMode()
            } else {
                Log.e("ChatViewModel", "Failed to delete selected chats.")
            }
        }
    }

    fun getRequestDetails(requestId: String) {
        viewModelScope.launch {
            repository.getRequestStream(requestId).collect { request ->
                _currentRequest.value = request
            }
        }
    }

    fun markJobAsFinished(request: HelpRequest) {
        val helperName = request.responderName ?: "The helper"
        val systemMessage = Message(
            senderId = "system",
            text = "$helperName has marked this request as complete. Awaiting confirmation."
        )
        viewModelScope.launch {
            repository.markRequestAsPendingApproval(request.id, systemMessage)
        }
    }

    // --- NEW: Function for the requester ---
    /**
     * Called by the requester to confirm the job is complete.
     */
    fun confirmCompletion(request: HelpRequest) {
        val requesterName = request.userId // In a real app, you'd fetch the requester's name
        val systemMessage = Message(
            senderId = "system",
            text = "The request has been confirmed as complete."
        )
        viewModelScope.launch {
            repository.completeRequest(request.id, systemMessage)
            // UI will update automatically.
        }
    }
    // --- END NEW ---

    fun clearChatScreenState() {
        _messages.value = emptyList()
        _currentRequest.value = null
    }
}
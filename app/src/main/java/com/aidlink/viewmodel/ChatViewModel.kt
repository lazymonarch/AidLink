package com.aidlink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.ChatWithStatus
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

    // --- NEW: State for managing selection mode ---
    private val _isInSelectionMode = MutableStateFlow(false)
    val isInSelectionMode: StateFlow<Boolean> = _isInSelectionMode.asStateFlow()

    private val _selectedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedChatIds: StateFlow<Set<String>> = _selectedChatIds.asStateFlow()
    // --- END NEW ---

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

    // --- NEW: Functions to manage selection state ---

    /**
     * Toggles the selection mode on or off.
     * When turning off, it clears any existing selections.
     */
    fun toggleSelectionMode() {
        _isInSelectionMode.update { !it }
        if (!_isInSelectionMode.value) {
            _selectedChatIds.value = emptySet()
        }
    }

    /**
     * Adds or removes a chat from the selection set.
     */
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
                // Exit selection mode and clear the selection list after a successful deletion.
                toggleSelectionMode()
            } else {
                // Optionally, you can add a new state to show an error message to the user.
                Log.e("ChatViewModel", "Failed to delete selected chats.")
            }
        }
    }
    // --- END NEW ---
}
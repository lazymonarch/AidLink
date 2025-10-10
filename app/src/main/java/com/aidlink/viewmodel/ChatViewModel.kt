package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.Chat
import com.aidlink.model.Message
import com.aidlink.utils.authStateFlow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        // CORRECTED: This now reliably fetches data on login
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
}
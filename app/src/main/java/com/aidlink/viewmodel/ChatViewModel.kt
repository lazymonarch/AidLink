package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.Chat
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val chats: StateFlow<List<Chat>> = repository.getAuthStateFlow()
        .flatMapLatest { user -> // Renamed from isLoggedIn for clarity
            if (user != null) { // The check is now for nullness
                repository.getChats()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _currentRequest = MutableStateFlow<HelpRequest?>(null)
    val currentRequest: StateFlow<HelpRequest?> = _currentRequest.asStateFlow()

    fun fetchMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId).collect { messageList ->
                _messages.value = messageList
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        val senderId = repository.getCurrentUser()?.uid
        if (senderId == null || text.isBlank()) return

        val message = Message(senderId = senderId, text = text)
        viewModelScope.launch {
            repository.sendMessage(chatId, message)
        }
    }

    fun markJobAsFinished(request: HelpRequest) {
        viewModelScope.launch {
            repository.enqueueRequestAction(request.id, "mark_complete")
        }
    }

    fun confirmCompletion(request: HelpRequest) {
        viewModelScope.launch {
            repository.enqueueRequestAction(request.id, "confirm_complete")
        }
    }

    fun getRequestDetails(requestId: String) {
        viewModelScope.launch {
            _currentRequest.value = repository.getRequestById(requestId)
        }
    }

    fun clearChatScreenState() {
        _messages.value = emptyList()
        _currentRequest.value = null
    }
}
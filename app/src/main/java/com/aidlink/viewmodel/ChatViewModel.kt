// In: main/java/com/aidlink/viewmodel/ChatViewModel.kt

package com.aidlink.viewmodel

import androidx.lifecycle.SavedStateHandle
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
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: StateFlow<String> = savedStateHandle.getStateFlow("chatId", "")
    val chats: StateFlow<List<Chat>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user != null) repository.getChats() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<Message>> = chatId.flatMapLatest { id ->
        if (id.isNotBlank()) {
            repository.getMessages(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentRequest = MutableStateFlow<HelpRequest?>(null)
    val currentRequest: StateFlow<HelpRequest?> = _currentRequest.asStateFlow()

    init {
        viewModelScope.launch {
            chatId.collect { id ->
                if (id.isNotBlank()) {
                    getRequestDetails(id)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val currentChatId = chatId.value
        if (currentChatId.isBlank() || text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(currentChatId, text)
        }
    }

    fun markJobAsFinished() {
        val request = _currentRequest.value ?: return
        viewModelScope.launch {
            repository.markJobAsComplete(request.id)
        }
    }

    fun confirmCompletion() {
        val request = _currentRequest.value ?: return
        viewModelScope.launch {
            repository.confirmCompletion(request.id)
        }
    }

    private fun getRequestDetails(requestId: String) {
        viewModelScope.launch {
            _currentRequest.value = repository.getRequestById(requestId)
        }
    }

    fun clearChatScreenState() {
        _currentRequest.value = null
    }
}

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

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val userName: String = savedStateHandle["userName"] ?: "User"
    
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _otherUserTyping = MutableStateFlow(false)
    val otherUserTyping = _otherUserTyping.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _request = MutableStateFlow<HelpRequest?>(null)
    val request = _request.asStateFlow()

    val chat: StateFlow<Chat?> = repository.getChatStream(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val messages: StateFlow<List<Message>> = repository.getMessages(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val currentUserId: StateFlow<String?> = flow {
        emit(repository.getCurrentUserId())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isHelper: StateFlow<Boolean> = combine(chat, currentUserId) { currentChat, userId ->
        currentChat?.helperId == userId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val isRequester: StateFlow<Boolean> = combine(chat, currentUserId) { currentChat, userId ->
        currentChat?.requesterId == userId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog = _showCompletionDialog.asStateFlow()

    init {
        // Comment out typing status to prevent crashes
        // observeTypingStatus()
        markMessagesAsRead()
        observeCompletionRequests()
    }

    private fun observeTypingStatus() {
        viewModelScope.launch {
            // repository.observeTypingStatus(chatId).collect { isTyping ->
            //     _otherUserTyping.value = isTyping
            // }
        }
    }
    
    private fun observeCompletionRequests() {
        viewModelScope.launch {
            combine(chat, isRequester) { currentChat, requester ->
                currentChat?.requestStatus == "pending_completion" && requester
            }.collect { shouldShow ->
                _showCompletionDialog.value = shouldShow
            }
        }
    }

    private fun markMessagesAsRead() {
        viewModelScope.launch {
            repository.markChatAsRead(chatId)
        }
    }
    
    fun loadRequestDetails(requestId: String) {
        viewModelScope.launch {
            try {
                val requestDetails = repository.getRequestById(requestId)
                _request.value = requestDetails
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load request details"
            }
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank()) return
        
        viewModelScope.launch {
            _isSending.value = true
            val success = repository.sendMessage(chatId, text)
            if (success) {
                _messageText.value = ""
            } else {
                _errorMessage.value = "Failed to send message"
            }
            _isSending.value = false
        }
    }

    fun markJobAsComplete() {
        val requestId = chat.value?.requestId ?: return
        viewModelScope.launch {
            try {
                val success = repository.markJobAsComplete(requestId)
                if (!success) {
                    _errorMessage.value = "Failed to mark job as complete"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun markJobAsNotComplete() {
        val requestId = chat.value?.requestId ?: return
        viewModelScope.launch {
            try {
                val success = repository.markJobAsNotComplete(requestId)
                if (success) {
                    repository.sendMessage(chatId, "Job marked as not complete")
                } else {
                    _errorMessage.value = "Failed to update status"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun confirmCompletion() {
        val requestId = chat.value?.requestId ?: return
        viewModelScope.launch {
            try {
                val success = repository.confirmCompletion(requestId)
                if (success) {
                    _showCompletionDialog.value = false
                } else {
                    _errorMessage.value = "Failed to confirm completion"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun rejectCompletion() {
        val requestId = chat.value?.requestId ?: return
        viewModelScope.launch {
            try {
                val success = repository.rejectJobCompletion(requestId)
                if (success) {
                    _showCompletionDialog.value = false
                } else {
                    _errorMessage.value = "Failed to reject completion"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }
}

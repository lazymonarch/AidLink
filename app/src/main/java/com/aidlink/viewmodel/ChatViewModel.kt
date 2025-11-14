package com.aidlink.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for individual Chat Screen
 * Manages messages, typing indicators, and request completion
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Navigation arguments
    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val userName: String = savedStateHandle["userName"] ?: "User"

    // Message state
    val messages: StateFlow<List<Message>> = repository.getMessages(chatId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Message input state
    private val _messageText = MutableStateFlow("")
    val messageText = _messageText.asStateFlow()

    // Request state
    private val _currentRequest = MutableStateFlow<HelpRequest?>(null)
    val currentRequest: StateFlow<HelpRequest?> = _currentRequest.asStateFlow()

    // UI state
    private val _isSending = MutableStateFlow(false)
    val isSending = _isSending.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val _otherUserTyping = MutableStateFlow(false)
    val otherUserTyping = _otherUserTyping.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog = _showCompletionDialog.asStateFlow()

    // Typing indicator debounce
    private var typingJob: Job? = null
    private var typingIndicatorJob: Job? = null

    // Scroll state
    private val _shouldScrollToBottom = MutableStateFlow(false)
    val shouldScrollToBottom = _shouldScrollToBottom.asStateFlow()

    init {
        loadRequestDetails()
        observeTypingStatus()
        markMessagesAsRead()
    }

    /**
     * Load request details associated with this chat
     */
    private fun loadRequestDetails() {
        viewModelScope.launch {
            try {
                // Extract request ID from chat ID or fetch from chat metadata
                // This assumes chatId contains or references the requestId
                val request = repository.getRequestById(chatId)
                _currentRequest.value = request
            } catch (e: Exception) {
                // Chat might not be associated with a request
                _currentRequest.value = null
            }
        }
    }

    /**
     * Observe typing status of other user
     */
    private fun observeTypingStatus() {
        viewModelScope.launch {
            try {
                repository.observeTypingStatus(chatId).collect { isTyping ->
                    _otherUserTyping.value = isTyping
                }
            } catch (e: Exception) {
                // Typing status not critical, silently fail
            }
        }
    }

    /**
     * Mark all messages in this chat as read
     */
    private fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                repository.markChatAsRead(chatId)
            } catch (e: Exception) {
                // Non-critical error
            }
        }
    }

    // ============================================================
    // Message Functions
    // ============================================================

    /**
     * Update message text and trigger typing indicator
     */
    fun updateMessageText(text: String) {
        _messageText.value = text
        
        // Update typing status
        if (text.isNotBlank()) {
            startTypingIndicator()
        } else {
            stopTypingIndicator()
        }
    }

    /**
     * Send message to chat
     */
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isBlank() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            try {
                val success = repository.sendMessage(chatId, text)
                
                if (success) {
                    _messageText.value = ""
                    _shouldScrollToBottom.value = true
                    stopTypingIndicator()
                } else {
                    _errorMessage.value = "Failed to send message"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Start typing indicator for other user
     */
    private fun startTypingIndicator() {
        // Cancel existing job
        typingIndicatorJob?.cancel()
        
        // Start new typing indicator
        typingIndicatorJob = viewModelScope.launch {
            _isTyping.value = true
            repository.setTypingStatus(chatId, true)
            
            // Auto-stop after 3 seconds of no activity
            delay(3000)
            stopTypingIndicator()
        }
    }

    /**
     * Stop typing indicator
     */
    private fun stopTypingIndicator() {
        typingIndicatorJob?.cancel()
        _isTyping.value = false
        
        viewModelScope.launch {
            repository.setTypingStatus(chatId, false)
        }
    }

    /**
     * Resend failed message
     */
    fun resendMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.resendMessage(chatId, messageId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to resend: ${e.message}"
            }
        }
    }

    /**
     * Delete message
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(chatId, messageId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete message: ${e.message}"
            }
        }
    }

    // ============================================================
    // Request Completion Functions
    // ============================================================

    /**
     * Mark job as finished (by helper)
     */
    fun markJobAsFinished() {
        val request = _currentRequest.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.markJobAsComplete(request.id)
                if (success) {
                    _showCompletionDialog.value = true
                } else {
                    _errorMessage.value = "Failed to mark job as complete"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Confirm completion (by requester)
     */
    fun confirmCompletion() {
        val request = _currentRequest.value ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.confirmCompletion(request.id)
                if (success) {
                    _showCompletionDialog.value = false
                    // Navigate to review screen handled by UI
                } else {
                    _errorMessage.value = "Failed to confirm completion"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Dismiss completion dialog
     */
    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    // ============================================================
    // Media & Attachments (Future Enhancement)
    // ============================================================

    /**
     * Send image message
     */
    fun sendImageMessage(imageUri: String) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                repository.sendImageMessage(chatId, imageUri)
                _shouldScrollToBottom.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send image: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Send location message
     */
    fun sendLocationMessage(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                _isSending.value = true
                repository.sendLocationMessage(chatId, latitude, longitude)
                _shouldScrollToBottom.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send location: ${e.message}"
            } finally {
                _isSending.value = false
            }
        }
    }

    // ============================================================
    // Utility Functions
    // ============================================================

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reset scroll flag
     */
    fun resetScrollFlag() {
        _shouldScrollToBottom.value = false
    }

    /**
     * Clear chat screen state on navigation away
     */
    fun clearChatScreenState() {
        _currentRequest.value = null
        _messageText.value = ""
        _errorMessage.value = null
        stopTypingIndicator()
    }

    /**
     * Get current user ID for message rendering
     */
    val currentUserId: StateFlow<String?> = flow {
        emit(repository.getCurrentUserId())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Check if current user is the helper
     */
    val isHelper: StateFlow<Boolean> = combine(
        currentRequest,
        currentUserId
    ) { request, userId ->
        request?.responderId == userId
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    /**
     * Check if current user is the requester
     */
    val isRequester: StateFlow<Boolean> = combine(
        currentRequest,
        currentUserId
    ) { request, userId ->
        request?.userId == userId
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    override fun onCleared() {
        super.onCleared()
        typingJob?.cancel()
        typingIndicatorJob?.cancel()
        stopTypingIndicator()
    }
}

/**
 * UI State for ChatScreen
 * Alternative sealed class approach
 */
sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(
        val messages: List<Message>,
        val request: HelpRequest?,
        val otherUserTyping: Boolean = false
    ) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
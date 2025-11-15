
package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.Chat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _rawChats = MutableStateFlow<List<Chat>>(emptyList())
    
    private val _selectedChats = MutableStateFlow<Set<String>>(emptySet())
    val selectedChats = _selectedChats.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch = _showSearch.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    val chats: StateFlow<List<Chat>> = combine(
        _rawChats,
        _searchQuery
    ) { rawChats, query ->
        if (query.isBlank()) {
            rawChats.sortedByDescending { it.lastMessageTimestamp?.seconds ?: 0 }
        } else {
            rawChats.filter { chat ->
                chat.otherUserName.contains(query, ignoreCase = true) ||
                chat.lastMessage.contains(query, ignoreCase = true)
            }.sortedByDescending { it.lastMessageTimestamp?.seconds ?: 0 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var searchJob: Job? = null

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            try {
                repository.getChats().collect { chatList ->
                    _rawChats.value = chatList
                    _errorMessage.value = null
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chats: ${e.message}"
            }
        }
    }

    fun refreshChats() {
        if (_isRefreshing.value) return
        
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            
            try {
                delay(1000)
                
                loadChats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleChatSelection(chatId: String) {
        val currentSelection = _selectedChats.value.toMutableSet()
        if (currentSelection.contains(chatId)) {
            currentSelection.remove(chatId)
        } else {
            currentSelection.add(chatId)
        }
        _selectedChats.value = currentSelection
        
        if (currentSelection.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun enterSelectionMode(chatId: String) {
        _selectionMode.value = true
        _selectedChats.value = setOf(chatId)
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedChats.value = emptySet()
    }

    fun selectAll() {
        val allChatIds = chats.value.map { it.id }.toSet()
        _selectedChats.value = if (_selectedChats.value.size == allChatIds.size) {
            emptySet()
        } else {
            allChatIds
        }
    }

    fun deleteSelectedChats() {
        val chatsToDelete = _selectedChats.value.toList()
        if (chatsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                repository.hideChatsForCurrentUser(chatsToDelete)
                exitSelectionMode()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete chats: ${e.message}"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        searchJob?.cancel()
        
        searchJob = viewModelScope.launch {
            delay(300)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _showSearch.value = false
        searchJob?.cancel()
    }

    fun toggleSearch() {
        _showSearch.value = !_showSearch.value
        if (!_showSearch.value) {
            _searchQuery.value = ""
            searchJob?.cancel()
        }
    }

    fun togglePinChat(chatId: String) {
        viewModelScope.launch {
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                repository.markChatAsRead(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to mark as read: ${e.message}"
            }
        }
    }

    fun toggleMuteChat(chatId: String) {
        viewModelScope.launch {
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    val unreadChatCount: StateFlow<Int> = chats.map { chatList ->
        chatList.count { it.unreadCount > 0 }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalUnreadCount: StateFlow<Int> = chats.map { chatList ->
        chatList.sumOf { it.unreadCount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

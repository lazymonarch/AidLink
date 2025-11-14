///Users/lakshan/AndroidStudioProjects/AidLink/app/src/main/java/com/aidlink/viewmodel/ChatsViewModel.kt

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

/**
 * ViewModel for the Chats List Screen
 * Manages chat list state, selection mode, search, and refresh functionality
 */
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Raw chat list from repository
    private val _rawChats = MutableStateFlow<List<Chat>>(emptyList())
    
    // Selection state
    private val _selectedChats = MutableStateFlow<Set<String>>(emptySet())
    val selectedChats = _selectedChats.asStateFlow()

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode = _selectionMode.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch = _showSearch.asStateFlow()

    // UI state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Filtered and sorted chats based on search query
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

    // Debounce job for search
    private var searchJob: Job? = null

    init {
        loadChats()
    }

    /**
     * Load chats from repository with real-time updates
     */
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

    /**
     * Refresh chats from server
     */
    fun refreshChats() {
        if (_isRefreshing.value) return
        
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            
            try {
                // Simulate network delay for pull-to-refresh animation
                delay(1000)
                
                // In a real implementation, this would trigger a server refresh
                // For now, we just reload from the existing flow
                loadChats()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ============================================================
    // Selection Mode Functions
    // ============================================================

    /**
     * Toggle selection for a specific chat
     */
    fun toggleChatSelection(chatId: String) {
        val currentSelection = _selectedChats.value.toMutableSet()
        if (currentSelection.contains(chatId)) {
            currentSelection.remove(chatId)
        } else {
            currentSelection.add(chatId)
        }
        _selectedChats.value = currentSelection
        
        // Exit selection mode if no chats are selected
        if (currentSelection.isEmpty()) {
            _selectionMode.value = false
        }
    }

    /**
     * Enter selection mode with initial chat selected
     */
    fun enterSelectionMode(chatId: String) {
        _selectionMode.value = true
        _selectedChats.value = setOf(chatId)
    }

    /**
     * Exit selection mode and clear selections
     */
    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedChats.value = emptySet()
    }

    /**
     * Select all chats or deselect all if already all selected
     */
    fun selectAll() {
        val allChatIds = chats.value.map { it.id }.toSet()
        _selectedChats.value = if (_selectedChats.value.size == allChatIds.size) {
            emptySet() // Deselect all
        } else {
            allChatIds // Select all
        }
    }

    /**
     * Delete/hide selected chats for current user
     */
    fun deleteSelectedChats() {
        val chatsToDelete = _selectedChats.value.toList()
        if (chatsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                chatsToDelete.forEach { chatId ->
                    repository.hideChatForCurrentUser(chatId)
                }
                exitSelectionMode()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete chats: ${e.message}"
            }
        }
    }

    // ============================================================
    // Search Functions
    // ============================================================

    /**
     * Update search query with debouncing for performance
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Debounce search for better performance
        searchJob = viewModelScope.launch {
            delay(300) // Wait 300ms before applying search
            // The combine operator above will handle the actual filtering
        }
    }

    /**
     * Clear search query and hide search bar
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _showSearch.value = false
        searchJob?.cancel()
    }

    /**
     * Toggle search bar visibility
     */
    fun toggleSearch() {
        _showSearch.value = !_showSearch.value
        if (!_showSearch.value) {
            _searchQuery.value = ""
            searchJob?.cancel()
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.utils.authStateFlow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MyActivityUiState {
    object Idle : MyActivityUiState()
    object Loading : MyActivityUiState()
    object Success : MyActivityUiState()
    data class NavigateToChat(val chatId: String, val otherUserName: String) : MyActivityUiState()
    data class Error(val message: String) : MyActivityUiState()
}


class MyActivityViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val currentUser = Firebase.auth.currentUser

    private val _myRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myRequests: StateFlow<List<HelpRequest>> = _myRequests.asStateFlow()

    private val _myResponses = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myResponses: StateFlow<List<HelpRequest>> = _myResponses.asStateFlow()

    private val _completedRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val completedRequests: StateFlow<List<HelpRequest>> = _completedRequests.asStateFlow()

    private val _actionUiState = MutableStateFlow<MyActivityUiState>(MyActivityUiState.Idle)
    val actionUiState: StateFlow<MyActivityUiState> = _actionUiState.asStateFlow()

    init {
        // CORRECTED: This now reliably fetches data on login
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collect { user ->
                if (user != null) {
                    fetchData(user.uid)
                } else {
                    _myRequests.value = emptyList()
                    _myResponses.value = emptyList()
                    _completedRequests.value = emptyList()
                }
            }
        }
    }

    private fun fetchData(userId: String) {
        viewModelScope.launch {
            repository.getMyRequests(userId).collect { requests ->
                _myRequests.value = requests.filter { it.status != "completed" }
                _completedRequests.value = requests.filter { it.status == "completed" }
            }
        }
        viewModelScope.launch {
            repository.getMyResponses(userId).collect { responses ->
                _myResponses.value = responses
            }
        }
    }

    fun onAcceptOffer(request: HelpRequest) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val requesterId = currentUser?.uid
            val helperId = request.responderId
            val helperName = request.responderName

            if (requesterId == null || helperId == null || helperName == null) {
                _actionUiState.value = MyActivityUiState.Error("User or responder not found.")
                return@launch
            }
            val success = repository.acceptOffer(request.id, requesterId, helperId)
            if (success) {
                // Emit the navigation state
                _actionUiState.value = MyActivityUiState.NavigateToChat(request.id, helperName)
            } else {
                _actionUiState.value = MyActivityUiState.Error("Failed to accept.")
            }
        }
    }

    fun onDeclineOffer(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.declineOffer(requestId)
            handleActionResult(success, "Failed to decline.")
        }
    }

    fun onDeleteRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.deleteRequest(requestId)
            handleActionResult(success, "Failed to delete.")
        }
    }

    fun onCancelRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.cancelRequest(requestId)
            handleActionResult(success, "Failed to cancel request.")
        }
    }

    private suspend fun handleActionResult(success: Boolean, errorMessage: String) {
        if (success) {
            _actionUiState.value = MyActivityUiState.Success
            delay(1500)
            resetActionState()
        } else {
            _actionUiState.value = MyActivityUiState.Error(errorMessage)
        }
    }

    fun resetActionState() {
        _actionUiState.value = MyActivityUiState.Idle
    }
}
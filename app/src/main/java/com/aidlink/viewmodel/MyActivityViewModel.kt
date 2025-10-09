package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.utils.authStateFlow // Using your custom extension
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyActivityViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _myRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myRequests: StateFlow<List<HelpRequest>> = _myRequests.asStateFlow()

    private val _myResponses = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myResponses: StateFlow<List<HelpRequest>> = _myResponses.asStateFlow()

    private val _completedRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val completedRequests: StateFlow<List<HelpRequest>> = _completedRequests.asStateFlow()

    private val _actionUiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val actionUiState: StateFlow<RequestUiState> = _actionUiState.asStateFlow()

    init {
        // CORRECTED: Observe the auth state to fetch data reliably
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collect { user ->
                if (user != null) {
                    fetchData(user.uid)
                } else {
                    // If user logs out, clear all data
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
            _actionUiState.value = RequestUiState.Loading
            val requesterId = Firebase.auth.currentUser?.uid
            val helperId = request.responderId
            if (requesterId == null || helperId == null) {
                _actionUiState.value = RequestUiState.Error("User or responder not found.")
                return@launch
            }
            val success = repository.acceptOffer(request.id, requesterId, helperId)
            handleActionResult(success, "Failed to accept.")
        }
    }

    fun onDeclineOffer(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = RequestUiState.Loading
            val success = repository.declineOffer(requestId)
            handleActionResult(success, "Failed to decline.")
        }
    }

    fun onDeleteRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = RequestUiState.Loading
            val success = repository.deleteRequest(requestId)
            handleActionResult(success, "Failed to delete.")
        }
    }

    fun onCancelRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = RequestUiState.Loading
            val success = repository.cancelRequest(requestId)
            // IMPROVED: Use the helper function for consistency
            handleActionResult(success, "Failed to cancel request.")
        }
    }

    private suspend fun handleActionResult(success: Boolean, errorMessage: String) {
        if (success) {
            _actionUiState.value = RequestUiState.Success
            delay(1500)
            resetActionState()
        } else {
            _actionUiState.value = RequestUiState.Error(errorMessage)
        }
    }

    fun resetActionState() {
        _actionUiState.value = RequestUiState.Idle
    }
}
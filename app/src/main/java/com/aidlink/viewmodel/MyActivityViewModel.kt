package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MyActivityViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val currentUser = Firebase.auth.currentUser
    private val _actionUiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val actionUiState: StateFlow<RequestUiState> = _actionUiState.asStateFlow()

    private val _myRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myRequests: StateFlow<List<HelpRequest>> = _myRequests.asStateFlow()

    private val _myResponses = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myResponses: StateFlow<List<HelpRequest>> = _myResponses.asStateFlow()

    private val _completedRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val completedRequests: StateFlow<List<HelpRequest>> = _completedRequests.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                // Fetch the user's own requests
                repository.getMyRequests(userId).collect { requests ->
                    // Here you would filter by status. For now, we'll show all.
                    _myRequests.value = requests.filter { it.status != "completed" } // Example filter
                    _completedRequests.value = requests.filter { it.status == "completed" } // Example filter
                }
            }
            viewModelScope.launch {
                // Fetch the user's responses (this will be empty for now)
                repository.getMyResponses(userId).collect { responses ->
                    _myResponses.value = responses
                }
            }
        }
    }

    fun onAcceptOffer(request: HelpRequest) {
        viewModelScope.launch {
            _actionUiState.value = RequestUiState.Loading
            val requesterId = currentUser?.uid
            val helperId = request.responderId

            if (requesterId == null || helperId == null) {
                _actionUiState.value = RequestUiState.Error("User or responder not found.")
                return@launch
            }
            val success = repository.acceptOffer(request.id, requesterId, helperId)
            if (success) {
                _actionUiState.value = RequestUiState.Success
                delay(2000) // Wait 2 seconds
                _actionUiState.value = RequestUiState.Idle
            } else {
                _actionUiState.value = RequestUiState.Error("Failed to accept.")
            }
        }
    }

    fun onDeclineOffer(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = RequestUiState.Loading
            val success = repository.declineOffer(requestId)
            if (success) {
                _actionUiState.value = RequestUiState.Success
                delay(2000) // Wait 2 seconds
                _actionUiState.value = RequestUiState.Idle
            } else {
                _actionUiState.value = RequestUiState.Error("Failed to decline.")
            }
        }
    }

    fun resetActionState() {
        _actionUiState.value = RequestUiState.Idle
    }
}
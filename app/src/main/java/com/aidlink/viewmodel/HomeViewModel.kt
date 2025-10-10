package com.aidlink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.utils.authStateFlow
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class RequestUiState {
    object Idle : RequestUiState()
    object Loading : RequestUiState()
    object Success : RequestUiState()
    data class Error(val message: String) : RequestUiState()
}

class HomeViewModel : ViewModel() {

    private val tag = "HomeViewModel"
    private val repository = AuthRepository()

    private val _requests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val requests: StateFlow<List<HelpRequest>> = _requests.asStateFlow()

    private val _selectedRequest = MutableStateFlow<HelpRequest?>(null)
    val selectedRequest: StateFlow<HelpRequest?> = _selectedRequest.asStateFlow()

    private val _requestUiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val requestUiState = _requestUiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Observe the user's login state
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collect { user ->
                if (user != null) {
                    fetchRequests() // Initial fetch on login
                } else {
                    _requests.value = emptyList()
                }
            }
        }
    }

    fun fetchRequests() {
        val userId = Firebase.auth.currentUser?.uid ?: return // Safety check

        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getRequests()
                .catch { exception ->
                    Log.e(tag, "Error fetching requests", exception)
                    _isRefreshing.value = false // Ensure refreshing stops on error
                }
                .collect { requestList ->
                    _requests.value = requestList.filter { it.userId != userId }
                    _isRefreshing.value = false // Mark refreshing as complete
                }
        }
    }

    fun postRequest(title: String, description: String, category: String, compensation: String) {
        viewModelScope.launch {
            _requestUiState.value = RequestUiState.Loading
            val currentUser = Firebase.auth.currentUser

            if (currentUser == null) {
                _requestUiState.value = RequestUiState.Error("User not logged in.")
                return@launch
            }

            val requestData = mapOf(
                "userId" to currentUser.uid,
                "title" to title,
                "description" to description,
                "category" to category,
                "compensation" to compensation,
                "status" to "open",
                "createdAt" to Timestamp.now()
            )

            Log.d(tag, "Attempting to save request: $requestData")
            val success = repository.saveRequest(requestData)

            if (success) {
                _requestUiState.value = RequestUiState.Success
            } else {
                _requestUiState.value = RequestUiState.Error("Failed to post request.")
            }
        }
    }

    fun onRespondToRequest(requestId: String) {
        viewModelScope.launch {
            _requestUiState.value = RequestUiState.Loading
            val currentUser = repository.getCurrentUser()
            val userProfile = currentUser?.uid?.let { repository.getUserProfileOnce(it) }

            if (currentUser == null || userProfile == null) {
                _requestUiState.value = RequestUiState.Error("Could not identify user.")
                return@launch
            }

            val success = repository.addResponderToRequest(
                requestId = requestId,
                responderId = currentUser.uid,
                responderName = userProfile.name
            )

            if (success) {
                _requestUiState.value = RequestUiState.Success
            } else {
                _requestUiState.value = RequestUiState.Error("Failed to send offer.")
            }
        }
    }

    fun getRequestById(requestId: String) {
        _selectedRequest.value = _requests.value.find { it.id == requestId }
    }

    fun resetRequestState() {
        _requestUiState.value = RequestUiState.Idle
    }
}
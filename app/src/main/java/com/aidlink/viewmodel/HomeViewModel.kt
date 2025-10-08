package com.aidlink.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
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

    private val TAG = "HomeViewModel"
    private val repository = AuthRepository()

    private val _requestUiState = MutableStateFlow<RequestUiState>(RequestUiState.Idle)
    val requestUiState = _requestUiState.asStateFlow()
    private val _requests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val requests: StateFlow<List<HelpRequest>> = _requests.asStateFlow()

    init {
        fetchRequests()
    }

    private fun fetchRequests() {
        viewModelScope.launch {
            repository.getRequests()
                .catch { exception ->
                    Log.e(TAG, "Error fetching requests", exception)
                }
                .collect { requestList ->
                    _requests.value = requestList
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

            Log.d(TAG, "Attempting to save request: $requestData")
            val success = repository.saveRequest(requestData)

            if (success) {
                _requestUiState.value = RequestUiState.Success
            } else {
                _requestUiState.value = RequestUiState.Error("Failed to post request.")
            }
        }
    }

    fun resetRequestState() {
        _requestUiState.value = RequestUiState.Idle
    }
}
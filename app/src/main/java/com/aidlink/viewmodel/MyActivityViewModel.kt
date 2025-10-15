package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyActivityViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val myActivityRequests: StateFlow<List<HelpRequest>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user != null) {
                repository.getMyActivityRequests(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myRequests: StateFlow<List<HelpRequest>> = myActivityRequests
        .map { requests ->
            requests.filter { it.userId == repository.getCurrentUser()?.uid && it.status != "completed" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myResponses: StateFlow<List<HelpRequest>> = myActivityRequests
        .map { requests ->
            requests.filter { it.responderId == repository.getCurrentUser()?.uid && it.status != "completed" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedRequests: StateFlow<List<HelpRequest>> = myActivityRequests
        .map { requests ->
            requests.filter { it.status == "completed" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onAcceptOffer(requestId: String, helperId: String) {
        viewModelScope.launch {
            repository.acceptOffer(requestId, helperId)
        }
    }

    // ---
    // --- THIS IS THE CRITICAL FIX ---
    // This function now correctly calls the repository to delete the request.
    fun onDeleteRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteRequest(requestId)
        }
    }
    // --- END OF FIX ---

    fun onCancelRequest(requestId: String) {
        viewModelScope.launch {
            repository.cancelRequest(requestId)
        }
    }

    fun onConfirmCompletion(request: HelpRequest) {
        viewModelScope.launch {
            repository.confirmCompletion(request.id)
        }
    }
}
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
        .flatMapLatest { isLoggedIn ->
            if (isLoggedIn) {
                repository.getMyActivityRequests()
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myRequests: StateFlow<List<HelpRequest>> = myActivityRequests.map { requests ->
        requests.filter { it.userId == repository.getCurrentUser()?.uid && it.status != "completed" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myResponses: StateFlow<List<HelpRequest>> = myActivityRequests.map { requests ->
        requests.filter { it.responderId == repository.getCurrentUser()?.uid && it.status != "completed" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedRequests: StateFlow<List<HelpRequest>> = myActivityRequests.map { requests ->
        requests.filter { it.status == "completed" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onAcceptOffer(request: HelpRequest) {
        viewModelScope.launch {
            repository.requestAction(request.id, "accept")
        }
    }

    fun onDeclineOffer(request: HelpRequest) {
        viewModelScope.launch {
            repository.requestAction(request.id, "decline")
        }
    }

    fun onDeleteRequest(requestId: String) { /* Not yet implemented */ }
    fun onCancelRequest(requestId: String) { /* Not yet implemented */ }

    fun onConfirmCompletion(request: HelpRequest) {
        viewModelScope.launch {
            repository.requestAction(request.id, "confirm_complete")
        }
    }
}
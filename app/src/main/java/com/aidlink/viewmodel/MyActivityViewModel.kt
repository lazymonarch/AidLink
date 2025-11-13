///Users/lakshan/AndroidStudioProjects/AidLink/app/src/main/java/com/aidlink/viewmodel/MyActivityViewModel.kt

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

    private val _refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val myActivityRequests: StateFlow<List<HelpRequest>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            // Combine with refresh trigger to re-fetch data
            _refreshTrigger.map { user }.onStart { emit(user) }
        }
        .flatMapLatest { user ->
            if (user != null) {
                repository.getMyActivityRequests(user.uid)
            } else {
                flowOf(emptyList())
            }
        }
        .catch { emit(emptyList()) }
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
            refreshData() // Refresh data after action
        }
    }
    fun onDeleteRequest(requestId: String) {
        viewModelScope.launch {
            repository.deleteRequest(requestId)
            refreshData() // Refresh data after action
        }
    }

    fun onCancelRequest(requestId: String) {
        viewModelScope.launch {
            repository.cancelRequest(requestId)
            refreshData() // Refresh data after action
        }
    }

    fun onConfirmCompletion(request: HelpRequest) {
        viewModelScope.launch {
            repository.confirmCompletion(request.id)
            refreshData() // Refresh data after action
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
        }
    }
}
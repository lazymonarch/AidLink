package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Offer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PostRequestUiState {
    object Idle : PostRequestUiState()
    object Loading : PostRequestUiState()
    object Success : PostRequestUiState()
    data class Error(val message: String) : PostRequestUiState()
}

sealed class RespondUiState {
    object Idle : RespondUiState()
    object Loading : RespondUiState()
    object Success : RespondUiState()
    data class Error(val message: String) : RespondUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val requests: StateFlow<List<HelpRequest>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user != null) {
                repository.getOpenHelpRequests()
            } else {
                flowOf(emptyList())
            }
        }

        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRequest = MutableStateFlow<HelpRequest?>(null)
    val selectedRequest: StateFlow<HelpRequest?> = _selectedRequest.asStateFlow()

    private val _postRequestUiState = MutableStateFlow<PostRequestUiState>(PostRequestUiState.Idle)
    val postRequestUiState: StateFlow<PostRequestUiState> = _postRequestUiState.asStateFlow()

    private val _respondUiState = MutableStateFlow<RespondUiState>(RespondUiState.Idle)
    val respondUiState: StateFlow<RespondUiState> = _respondUiState.asStateFlow()

    val offers: StateFlow<List<Offer>> = selectedRequest
        .filterNotNull()
        .flatMapLatest { request ->
            repository.getOffers(request.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMakeOffer(requestId: String) {
        viewModelScope.launch {
            _respondUiState.value = RespondUiState.Loading
            val success = repository.makeOffer(requestId)
            _respondUiState.value = if (success) RespondUiState.Success else RespondUiState.Error("Failed to send offer.")
        }
    }

    fun resetRespondState() {
        _respondUiState.value = RespondUiState.Idle
    }

    fun getRequestById(requestId: String) {
        viewModelScope.launch {
            _selectedRequest.value = repository.getRequestById(requestId)
        }
    }

    fun postRequest(title: String, description: String, category: String, compensation: String) {
        viewModelScope.launch {
            _postRequestUiState.value = PostRequestUiState.Loading
            val currentUser = repository.getCurrentUser() ?: run {
                _postRequestUiState.value = PostRequestUiState.Error("You must be logged in.")
                return@launch
            }
            val userProfile = repository.getUserProfileOnce(currentUser.uid) ?: run {
                _postRequestUiState.value = PostRequestUiState.Error("Could not load your profile to post.")
                return@launch
            }
            val newRequest = HelpRequest(
                userId = currentUser.uid,
                userName = userProfile.name,
                title = title,
                description = description,
                category = category,
                location = "User's Area",
                type = if (compensation == "Fee") com.aidlink.model.RequestType.FEE else com.aidlink.model.RequestType.VOLUNTEER,
                status = "open"
            )
            val success = repository.createRequest(newRequest)
            _postRequestUiState.value = if (success) PostRequestUiState.Success else PostRequestUiState.Error("Failed to post request.")
        }
    }

    fun resetPostRequestState() {
        _postRequestUiState.value = PostRequestUiState.Idle
    }
}
package com.aidlink.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditRequestUiState {
    object Idle : EditRequestUiState()
    object Loading : EditRequestUiState()
    object Success : EditRequestUiState()
    data class Error(val message: String) : EditRequestUiState()
}

@HiltViewModel
class EditRequestViewModel @Inject constructor(
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val requestId: String = savedStateHandle.get<String>("requestId")!!

    private val _request = MutableStateFlow<HelpRequest?>(null)
    val request: StateFlow<HelpRequest?> = _request.asStateFlow()

    private val _editRequestUiState = MutableStateFlow<EditRequestUiState>(EditRequestUiState.Idle)
    val editRequestUiState: StateFlow<EditRequestUiState> = _editRequestUiState.asStateFlow()

    init {
        fetchRequestDetails()
    }

    private fun fetchRequestDetails() {
        viewModelScope.launch {
            _request.value = repository.getRequestById(requestId)
        }
    }

    fun onUpdateRequest(title: String, description: String, category: String, compensation: String) {
        viewModelScope.launch {
            _editRequestUiState.value = EditRequestUiState.Loading
            val currentRequest = _request.value ?: run {
                _editRequestUiState.value = EditRequestUiState.Error("Original request not found.")
                return@launch
            }

            val updatedRequest = currentRequest.copy(
                title = title,
                description = description,
                category = category,
                type = if (compensation == "Fee") RequestType.FEE else RequestType.VOLUNTEER
            )

            val success = repository.updateRequest(requestId, updatedRequest)
            _editRequestUiState.value = if (success) {
                EditRequestUiState.Success
            } else {
                EditRequestUiState.Error("Failed to update request.")
            }
        }
    }
}
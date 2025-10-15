package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DeleteAccountUiState {
    object Idle : DeleteAccountUiState()
    object Loading : DeleteAccountUiState()
    object Success : DeleteAccountUiState()
    data class Error(val message: String) : DeleteAccountUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _deleteAccountUiState = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Idle)
    val deleteAccountUiState: StateFlow<DeleteAccountUiState> = _deleteAccountUiState.asStateFlow()

    fun onDeleteAccountClicked() {
        viewModelScope.launch {
            _deleteAccountUiState.value = DeleteAccountUiState.Loading

            // Note: In a production app, this is where you would force re-authentication.
            // For now, we will proceed directly with the deletion.

            val success = repository.deleteUserAccount()

            _deleteAccountUiState.value = if (success) {
                // The user's auth state will change, triggering a navigation to the login screen.
                DeleteAccountUiState.Success
            } else {
                DeleteAccountUiState.Error("Failed to delete account. Please try again.")
            }
        }
    }

    fun resetState() {
        _deleteAccountUiState.value = DeleteAccountUiState.Idle
    }
}
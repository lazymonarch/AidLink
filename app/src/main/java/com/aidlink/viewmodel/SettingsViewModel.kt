
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

    /**
     * Initiates account deletion process
     */
    fun onDeleteAccountClicked() {
        viewModelScope.launch {
            _deleteAccountUiState.value = DeleteAccountUiState.Loading
            val success = repository.deleteUserAccount()
            _deleteAccountUiState.value = if (success) {
                DeleteAccountUiState.Success
            } else {
                DeleteAccountUiState.Error("Failed to delete account. Please try again.")
            }
        }
    }

    /**
     * Resets the delete account UI state back to Idle
     */
    fun resetState() {
        _deleteAccountUiState.value = DeleteAccountUiState.Idle
    }
    
    /**
     * Logs out the current user
     */
    fun logOut() {
        repository.signOut()
    }
}

package com.aidlink.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Loading : EditProfileUiState()
    object Success : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _editProfileUiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val editProfileUiState: StateFlow<EditProfileUiState> = _editProfileUiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val uid = repository.getCurrentUser()?.uid
            if (uid != null) {
                _userProfile.value = repository.getUserProfileOnce(uid)
            }
        }
    }

    fun onUpdateProfile(
        name: String,
        bio: String,
        skills: String,
        area: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _editProfileUiState.value = EditProfileUiState.Loading
            val uid = repository.getCurrentUser()?.uid
            if (uid == null) {
                _editProfileUiState.value = EditProfileUiState.Error("You are not logged in.")
                return@launch
            }

            // Convert the skills string into a list
            val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val success = repository.updateUserProfile(uid, name, bio, skillList, area, imageUri)

            _editProfileUiState.value = if (success) {
                EditProfileUiState.Success
            } else {
                EditProfileUiState.Error("Failed to update profile.")
            }
        }
    }
}
package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val currentUser = repository.getCurrentUser()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut = _isLoggedOut.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                repository.getUserProfile(userId)
                    .catch {
                        // Handle error
                    }
                    .collect { profile ->
                        _userProfile.value = profile
                    }
            }
        }
    }

    fun onLogoutClicked() {
        repository.logout()
        _isLoggedOut.value = true
    }
}
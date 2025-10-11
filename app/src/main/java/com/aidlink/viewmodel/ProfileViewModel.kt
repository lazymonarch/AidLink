package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val currentUser = repository.getCurrentUser()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    // --- NEW: StateFlows for user stats ---
    private val _helpsCount = MutableStateFlow(0)
    val helpsCount: StateFlow<Int> = _helpsCount.asStateFlow()

    private val _requestsCount = MutableStateFlow(0)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()
    // --- END NEW ---

    init {
        fetchUserProfile()
        fetchUserStats()
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

    // --- NEW: Function to fetch user stats ---
    private fun fetchUserStats() {
        currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                repository.getUserHelpsCount(userId).collect { count ->
                    _helpsCount.value = count
                }
            }
            viewModelScope.launch {
                repository.getUserRequestsCount(userId).collect { count ->
                    _requestsCount.value = count
                }
            }
        }
    }
    // --- END NEW ---

    fun onLogoutClicked() {
        repository.logout()
        _isLoggedOut.value = true
    }
}
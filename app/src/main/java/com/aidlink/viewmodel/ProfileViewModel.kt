package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.getAuthStateFlow()
        .flatMapLatest { isLoggedIn ->
            val uid = repository.getCurrentUser()?.uid
            if (isLoggedIn && uid != null) {
                repository.getUserProfile(uid)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoggedOut = MutableStateFlow(false)
    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    fun onLogoutClicked() {
        repository.signOut()
        _isLoggedOut.value = true
    }
}
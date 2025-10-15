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
        .flatMapLatest { user ->
            if (user != null) {
                repository.getUserProfile(user.uid)
            } else {
                flowOf(null)
            }
        }

        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onLogoutClicked() {
        repository.signOut()
    }
}
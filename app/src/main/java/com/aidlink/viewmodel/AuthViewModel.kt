package com.aidlink.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val verificationId: String) : AuthUiState()
    object AuthSuccessExistingUser : AuthUiState() // For returning users
    object AuthSuccessNewUser : AuthUiState()      // For new users
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val tag = "AuthViewModel"
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()
    private val authRepository = AuthRepository()

    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.sendOtp(
                phone = phoneNumber,
                activity = activity,
                onCodeSent = { verificationId ->
                    _uiState.value = AuthUiState.OtpSent(verificationId)
                },
                onVerificationFailed = { exception ->
                    _uiState.value = AuthUiState.Error(exception.message ?: "Something went wrong")
                },
                onVerificationCompleted = { credential ->
                    signIn(credential)
                }
            )
        }
    }

    fun verifyOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val credential = authRepository.getCredential(verificationId, otp)
            signIn(credential)
        }
    }

    private fun signIn(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            Log.d(tag, "Calling repository.signInWithCredential...")
            val user = authRepository.signInWithCredential(credential)

            if (user != null) {
                Log.d(tag, "Sign-in successful. User UID: ${user.uid}. Checking profile...")
                val profileExists = authRepository.checkIfProfileExists(user.uid)
                if (profileExists) {
                    Log.d(tag, "Profile exists. Setting state to AuthSuccessExistingUser.")
                    _uiState.value = AuthUiState.AuthSuccessExistingUser
                } else {
                    Log.d(tag, "Profile does NOT exist. Setting state to AuthSuccessNewUser.")
                    _uiState.value = AuthUiState.AuthSuccessNewUser
                }
            } else {
                Log.e(tag, "Sign-in failed. User is null. Setting state to Error.")
                _uiState.value = AuthUiState.Error("Authentication failed.")
            }
        }
    }

    fun saveUserProfile(name: String, skills: List<String>, area: String) { // bio parameter removed
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val user = authRepository.getCurrentUser()
            if (user == null) {
                _uiState.value = AuthUiState.Error("No user is logged in.")
                return@launch
            }

            val userProfile = mapOf(
                "uid" to user.uid,
                "name" to name,
                "skills" to skills,
                "area" to area,
                "bio" to "", // Save bio as an empty string by default
                "phone" to (user.phoneNumber ?: "")
            )

            val success = authRepository.createUserProfile(user.uid, userProfile)

            if (success) {
                _uiState.value = AuthUiState.AuthSuccessExistingUser
            } else {
                _uiState.value = AuthUiState.Error("Failed to save profile.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
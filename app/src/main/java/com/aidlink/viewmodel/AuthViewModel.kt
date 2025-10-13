package com.aidlink.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.UserProfile
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpSent(val verificationId: String) : AuthUiState()
    object AuthSuccessExistingUser : AuthUiState()
    object AuthSuccessNewUser : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    fun onPhoneNumberChanged(newNumber: String) {
        _phoneNumber.value = newNumber
    }

    private fun setTemporaryError(message: String) {
        _uiState.value = AuthUiState.Error(message)
        viewModelScope.launch {
            delay(3000L)
            _uiState.value = AuthUiState.Idle
        }
    }

    fun sendOtp(activity: Activity) {
        _uiState.value = AuthUiState.Loading
        val fullPhoneNumber = "+91${_phoneNumber.value}"
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signIn(credential)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                setTemporaryError(e.message ?: "An unknown error occurred.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                _uiState.value = AuthUiState.OtpSent(verificationId)
            }
        }
        repository.sendVerificationCode(fullPhoneNumber, activity, callbacks)
    }

    fun verifyOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                signIn(credential)
            } catch (e: Exception) {
                setTemporaryError("Invalid OTP. Please try again.")
            }
        }
    }

    private fun signIn(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            val success = repository.signInWithPhoneAuthCredential(credential)
            if (success) {
                if (repository.isUserProfileExists()) {
                    _uiState.value = AuthUiState.AuthSuccessExistingUser
                } else {
                    _uiState.value = AuthUiState.AuthSuccessNewUser
                }
            } else {
                setTemporaryError("Authentication failed.")
            }
        }
    }

    fun saveUserProfile(name: String, skills: List<String>, area: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val user = repository.getCurrentUser() ?: return@launch
            val userProfile = UserProfile(
                uid = user.uid,
                name = name,
                skills = skills,
                area = area,
                phone = user.phoneNumber ?: ""
            )
            val success = repository.createUserProfile(userProfile)
            if (success) {
                _uiState.value = AuthUiState.AuthSuccessExistingUser
            } else {
                setTemporaryError("Failed to save profile.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
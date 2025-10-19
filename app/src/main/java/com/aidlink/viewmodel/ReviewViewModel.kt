package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Review
import com.aidlink.model.UserProfile
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val db = Firebase.firestore
    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _revieweeProfile = MutableStateFlow<UserProfile?>(null)
    val revieweeProfile: StateFlow<UserProfile?> = _revieweeProfile

    private val _currentRequest = MutableStateFlow<HelpRequest?>(null)
    val currentRequest: StateFlow<HelpRequest?> = _currentRequest

    fun fetchRevieweeProfile(userId: String) {
        viewModelScope.launch {
            _revieweeProfile.value = repository.getUserProfileOnce(userId)
        }
    }

    fun fetchRequest(requestId: String) {
        viewModelScope.launch {
            _currentRequest.value = repository.getRequestById(requestId)
        }
    }

    fun submitReview(review: Review) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Submitting
            try {
                db.collection("reviews").add(review).await()
                _uiState.value = ReviewUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReviewUiState.Error(e.message ?: "Failed to submit review.")
            }
        }
    }
}

sealed class ReviewUiState {
    object Idle : ReviewUiState()
    object Submitting : ReviewUiState()
    object Success : ReviewUiState()
    data class Error(val message: String) : ReviewUiState()
}
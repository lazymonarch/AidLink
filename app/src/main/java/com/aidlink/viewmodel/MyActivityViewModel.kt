package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Message
import com.aidlink.model.Review
import com.aidlink.utils.authStateFlow
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class MyActivityUiState {
    object Idle : MyActivityUiState()
    object Loading : MyActivityUiState()
    object Success : MyActivityUiState()
    data class NavigateToChat(val chatId: String, val otherUserName: String) : MyActivityUiState()
    data class Error(val message: String) : MyActivityUiState()
}
class MyActivityViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val _myRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myRequests: StateFlow<List<HelpRequest>> = _myRequests.asStateFlow()

    private val _myResponses = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myResponses: StateFlow<List<HelpRequest>> = _myResponses.asStateFlow()

    private val _completedRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val completedRequests: StateFlow<List<HelpRequest>> = _completedRequests.asStateFlow()

    private val _actionUiState = MutableStateFlow<MyActivityUiState>(MyActivityUiState.Idle)
    val actionUiState: StateFlow<MyActivityUiState> = _actionUiState.asStateFlow()

    private val _requestToReview = MutableStateFlow<HelpRequest?>(null)
    val requestToReview: StateFlow<HelpRequest?> = _requestToReview.asStateFlow()

    init {
        viewModelScope.launch {
            // Listen for user authentication state changes
            Firebase.auth.authStateFlow().collect { user ->
                if (user != null) {
                    // --- THIS IS THE CORRECTED DATA FETCHING LOGIC ---
                    // Combine the two flows into a single, unified stream
                    repository.getMyRequests(user.uid).combine(repository.getMyResponses(user.uid)) { myRequests, myResponses ->
                        // Filter for the "My Requests" tab (active items)
                        _myRequests.value = myRequests.filter { it.status != "completed" }

                        // Filter for the "My Responses" tab (active items)
                        _myResponses.value = myResponses.filter { it.status != "completed" }

                        // Filter and combine for the "Completed" tab
                        val completedFromMyRequests = myRequests.filter { it.status == "completed" }
                        val completedFromMyResponses = myResponses.filter { it.status == "completed" }
                        _completedRequests.value = (completedFromMyRequests + completedFromMyResponses)
                            .distinctBy { it.id } // Ensure no duplicates
                            .sortedByDescending { it.createdAt } // Sort the final list
                    }.collect() // Start collecting the combined flow
                } else {
                    // Clear all lists if the user logs out
                    _myRequests.value = emptyList()
                    _myResponses.value = emptyList()
                    _completedRequests.value = emptyList()
                }
            }
        }
    }

    // The fetchData function is no longer needed as logic is moved to the init block.

    fun onAcceptOffer(request: HelpRequest) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val requesterId = Firebase.auth.currentUser?.uid
            val helperId = request.responderId
            val helperName = request.responderName

            if (requesterId == null || helperId == null || helperName == null) {
                _actionUiState.value = MyActivityUiState.Error("User or responder not found.")
                return@launch
            }
            val success = repository.acceptOffer(request.id, requesterId, helperId)
            if (success) {
                _actionUiState.value = MyActivityUiState.NavigateToChat(request.id, helperName)
            } else {
                _actionUiState.value = MyActivityUiState.Error("Failed to accept.")
            }
        }
    }

    fun onDeclineOffer(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.declineOffer(requestId)
            handleActionResult(success, "Failed to decline.")
        }
    }

    fun onDeleteRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.deleteRequest(requestId)
            handleActionResult(success, "Failed to delete.")
        }
    }

    fun onCancelRequest(requestId: String) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val success = repository.cancelRequest(requestId)
            handleActionResult(success, "Failed to cancel request.")
        }
    }

    fun onConfirmCompletion(request: HelpRequest) {
        viewModelScope.launch {
            _actionUiState.value = MyActivityUiState.Loading
            val systemMessage = Message(
                senderId = "system",
                text = "The request has been confirmed as complete."
            )
            val success = repository.completeRequest(request.id, systemMessage)
            if (success) {
                _actionUiState.value = MyActivityUiState.Success
                delay(1500) // Wait for success message to show
                _actionUiState.value = MyActivityUiState.Idle
                _requestToReview.value = request // This will trigger the dialog
            } else {
                _actionUiState.value = MyActivityUiState.Error("Failed to confirm completion.")
            }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        viewModelScope.launch {
            val request = _requestToReview.value
            val currentUser = repository.getCurrentUser()
            val userProfile = currentUser?.uid?.let { repository.getUserProfileOnce(it) }

            if (request?.responderId == null || currentUser == null || userProfile == null) {
                _requestToReview.value = null // Close dialog
                return@launch
            }

            val review = Review(
                reviewerId = currentUser.uid,
                reviewerName = userProfile.name,
                rating = rating,
                comment = comment
            )

            repository.submitReview(request.responderId, review)
            _requestToReview.value = null // Close the dialog
        }
    }

    fun dismissReviewDialog() {
        _requestToReview.value = null
    }

    private suspend fun handleActionResult(success: Boolean, errorMessage: String) {
        if (success) {
            _actionUiState.value = MyActivityUiState.Success
            delay(1500)
            resetActionState()
        } else {
            _actionUiState.value = MyActivityUiState.Error(errorMessage)
        }
    }

    fun resetActionState() {
        _actionUiState.value = MyActivityUiState.Idle
    }
}
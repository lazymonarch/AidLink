package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyActivityViewModel : ViewModel() {

    private val repository = AuthRepository()
    private val currentUser = Firebase.auth.currentUser

    private val _myRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myRequests: StateFlow<List<HelpRequest>> = _myRequests.asStateFlow()

    private val _myResponses = MutableStateFlow<List<HelpRequest>>(emptyList())
    val myResponses: StateFlow<List<HelpRequest>> = _myResponses.asStateFlow()

    private val _completedRequests = MutableStateFlow<List<HelpRequest>>(emptyList())
    val completedRequests: StateFlow<List<HelpRequest>> = _completedRequests.asStateFlow()

    init {
        fetchData()
    }

    private fun fetchData() {
        currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                // Fetch the user's own requests
                repository.getMyRequests(userId).collect { requests ->
                    // Here you would filter by status. For now, we'll show all.
                    _myRequests.value = requests.filter { it.status != "completed" } // Example filter
                    _completedRequests.value = requests.filter { it.status == "completed" } // Example filter
                }
            }
            viewModelScope.launch {
                // Fetch the user's responses (this will be empty for now)
                repository.getMyResponses(userId).collect { responses ->
                    _myResponses.value = responses
                }
            }
        }
    }
}
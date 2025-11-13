package com.aidlink.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.RequestType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditRequestViewModel @Inject constructor(
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get request ID from navigation arguments
    private val requestId: String = checkNotNull(savedStateHandle["requestId"])

    // Event channel for one-time events
    private val _saveResult = MutableSharedFlow<Boolean>()
    val saveResult: SharedFlow<Boolean> = _saveResult.asSharedFlow()

    // Original request data (immutable, for comparison)
    private val _originalRequest = MutableStateFlow<HelpRequest?>(null)
    val originalRequest: StateFlow<HelpRequest?> = _originalRequest.asStateFlow()

    // Editable fields
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _compensationType = MutableStateFlow(RequestType.FEE)
    val compensationType: StateFlow<RequestType> = _compensationType.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    // UI state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Computed property: Check if there are unsaved changes
    val hasUnsavedChanges: StateFlow<Boolean> = combine(
        _originalRequest,
        _title,
        _description,
        _selectedCategory,
        _compensationType
    ) { original: HelpRequest?, title: String, desc: String, category: String, requestType: RequestType ->
        if (original == null) {
            false
        } else {
            title != original.title ||
            desc != original.description ||
            category != original.category ||
            requestType != original.type
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        loadRequest()
    }

    /**
     * Load the original request data from repository
     */
    private fun loadRequest() {
        viewModelScope.launch {
            try {
                val request = repository.getRequestById(requestId)
                request?.let { req ->
                    _originalRequest.value = req
                    // Initialize editable fields
                    _title.value = req.title
                    _description.value = req.description
                    _selectedCategory.value = req.category
                    _compensationType.value = req.type
                    _location.value = req.locationName
                }
            } catch (e: Exception) {
                // Handle error - could show snackbar in UI
            }
        }
    }

    /**
     * Update title field
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    /**
     * Update description field
     */
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }

    /**
     * Select a category
     */
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    /**
     * Set compensation type
     */
    fun setCompensationType(type: RequestType) {
        _compensationType.value = type
    }

    /**
     * Reset all fields to original values
     */
    fun resetChanges() {
        _originalRequest.value?.let { original ->
            _title.value = original.title
            _description.value = original.description
            _selectedCategory.value = original.category
            _compensationType.value = original.type
            _location.value = original.locationName
        }
    }

    /**
     * Save changes to repository
     */
    fun saveChanges() {
        val original = _originalRequest.value ?: return
        
        // Validation
        if (_title.value.isBlank() || _description.value.isBlank()) {
            return
        }

        viewModelScope.launch {
            _isSaving.value = true

            try {
                // Create updated request
                val updatedRequest = original.copy(
                    title = _title.value.trim(),
                    description = _description.value.trim(),
                    category = _selectedCategory.value,
                    type = _compensationType.value
                )

                // Update in repository - use correct method signature
                val success = repository.updateRequest(requestId, updatedRequest)

                if (success) {
                    // Update original to reflect saved state
                    _originalRequest.value = updatedRequest
                }

                _saveResult.emit(success)
            } catch (e: Exception) {
                _saveResult.emit(false)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
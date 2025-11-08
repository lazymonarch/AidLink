package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Offer
import com.github.davidmoten.geo.GeoHash
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PostRequestUiState {
    object Idle : PostRequestUiState()
    object Loading : PostRequestUiState()
    object Success : PostRequestUiState()
    data class Error(val message: String) : PostRequestUiState()
}

sealed class RespondUiState {
    object Idle : RespondUiState()
    object Loading : RespondUiState()
    object Success : RespondUiState()
    data class Error(val message: String) : PostRequestUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _userGeoPoint = MutableStateFlow<GeoPoint?>(null)
    val userGeoPoint: StateFlow<GeoPoint?> = _userGeoPoint.asStateFlow()
    private val _radiusKm = MutableStateFlow(5.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val requests: StateFlow<List<HelpRequest>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                repository.getUserProfile(user.uid).flatMapLatest { profile ->
                    // Use roundedLat/roundedLon if available, fallback to location
                    val userLocation = when {
                        profile?.roundedLat != null && profile.roundedLon != null -> {
                            GeoPoint(profile.roundedLat, profile.roundedLon)
                        }
                        profile?.location != null -> profile.location
                        else -> null
                    }
                    
                    android.util.Log.d("HomeViewModel", "User profile loaded: area=${profile?.area}, roundedLat=${profile?.roundedLat}, roundedLon=${profile?.roundedLon}, location=${profile?.location}")
                    android.util.Log.d("HomeViewModel", "Using userLocation: $userLocation")
                    
                    if (userLocation == null) {
                        android.util.Log.d("HomeViewModel", "No user location available, returning empty list")
                        flowOf(emptyList())
                    } else {
                        _userGeoPoint.value = userLocation
                        android.util.Log.d("HomeViewModel", "Querying nearby requests for location: lat=${userLocation.latitude}, lon=${userLocation.longitude}")
                        _radiusKm.flatMapLatest { radius ->
                            repository.getNearbyHelpRequests(userLocation, radius)
                        }
                    }
                }
            }
        }
        .catch {
            emit(emptyList())
            // TODO: Log this error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setRadius(km: Double) {
        _radiusKm.value = km
    }
    private val _selectedRequest = MutableStateFlow<HelpRequest?>(null)
    val selectedRequest: StateFlow<HelpRequest?> = _selectedRequest.asStateFlow()

    private val _postRequestUiState = MutableStateFlow<PostRequestUiState>(PostRequestUiState.Idle)
    val postRequestUiState: StateFlow<PostRequestUiState> = _postRequestUiState.asStateFlow()

    private val _respondUiState = MutableStateFlow<RespondUiState>(RespondUiState.Idle)
    val respondUiState: StateFlow<RespondUiState> = _respondUiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val offers: StateFlow<List<Offer>> = selectedRequest
        .filterNotNull()
        .flatMapLatest { request ->
            repository.getOffers(request.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMakeOffer(requestId: String) {
        viewModelScope.launch {
            _respondUiState.value = RespondUiState.Loading
            val success = repository.makeOffer(requestId)
            _respondUiState.value = (if (success) RespondUiState.Success else RespondUiState.Error("Failed to send offer.")) as RespondUiState
        }
    }

    fun resetRespondState() {
        _respondUiState.value = RespondUiState.Idle
    }

    fun getRequestById(requestId: String) {
        viewModelScope.launch {
            _selectedRequest.value = repository.getRequestById(requestId)
        }
    }

    fun postRequest(title: String, description: String, category: String, compensation: String) {
        viewModelScope.launch {
            _postRequestUiState.value = PostRequestUiState.Loading

            val currentUser = repository.getCurrentUser() ?: run {
                _postRequestUiState.value = PostRequestUiState.Error("You must be logged in.")
                return@launch
            }

            val userProfile = repository.getUserProfileOnce(currentUser.uid) ?: run {
                _postRequestUiState.value = PostRequestUiState.Error("Could not load your profile to post.")
                return@launch
            }

            val userGeoPoint = userProfile.location
            val locationName = userProfile.area

            if (userGeoPoint == null) {
                _postRequestUiState.value = PostRequestUiState.Error("Please set your location in your profile first.")
                return@launch
            }

            val lat = userGeoPoint.latitude
            val lon = userGeoPoint.longitude
            val geohash = GeoHash.encodeHash(lat, lon, 7) // 7 chars = ~150m precision

            val newRequest = HelpRequest(
                userId = currentUser.uid,
                userName = userProfile.name,
                title = title,
                description = description,
                category = category,
                locationName = locationName,
                type = if (compensation == "Fee") com.aidlink.model.RequestType.FEE else com.aidlink.model.RequestType.VOLUNTEER,
                status = "open",

                latitude = lat,
                longitude = lon,
                geohash = geohash
            )
            val success = repository.createRequest(newRequest)
            _postRequestUiState.value = if (success) PostRequestUiState.Success else PostRequestUiState.Error("Failed to post request.")
        }
    }

    fun resetPostRequestState() {
        _postRequestUiState.value = PostRequestUiState.Idle
    }
}
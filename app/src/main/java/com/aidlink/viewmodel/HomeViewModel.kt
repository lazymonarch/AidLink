///Users/lakshan/AndroidStudioProjects/AidLink/app/src/main/java/com/aidlink/viewmodel/HomeViewModel.kt

package com.aidlink.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.model.HelpRequest
import com.aidlink.model.Offer
import com.aidlink.utils.Geometries
import com.github.davidmoten.geo.GeoHash
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
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
    data class Error(val message: String) : RespondUiState()  // Fixed: Changed from PostRequestUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _userGeoPoint = MutableStateFlow<GeoPoint?>(null)
    val userGeoPoint: StateFlow<GeoPoint?> = _userGeoPoint.asStateFlow()
    private val _radiusKm = MutableStateFlow(10.0)

    val mapViewportState = MapViewportState().apply {
        setCameraOptions {
            center(Point.fromLngLat(80.2707, 13.0827))
            zoom(13.0)
        }
    }

    private val _centerMapOnUserAction = MutableStateFlow<GeoPoint?>(null)
    val centerMapOnUserAction: StateFlow<GeoPoint?> = _centerMapOnUserAction

    @OptIn(ExperimentalCoroutinesApi::class)
    val requests: StateFlow<List<HelpRequest>> = repository.getAuthStateFlow()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                repository.getUserProfile(user.uid).flatMapLatest { profile ->
                    val userLocation = profile?.location
                    if (userLocation == null) {
                        flowOf(emptyList())
                    } else {
                        _userGeoPoint.value = userLocation
                        if (_centerMapOnUserAction.value == null) {
                            _centerMapOnUserAction.value = userLocation
                        }
                        _radiusKm.flatMapLatest { radius ->
                            repository.getNearbyHelpRequests(userLocation, radius)
                        }
                    }
                }
            }
        }
        .catch {
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMapCentered() {
        _centerMapOnUserAction.value = null
    }

    fun resetMapToUserLocation() {
        val userLocation = _userGeoPoint.value
        if (userLocation != null) {
            mapViewportState.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(userLocation.longitude, userLocation.latitude))
                    .zoom(13.0)
                    .build()
            )
        }
    }

    fun setRadius(km: Double) {
        _radiusKm.value = km
    }

    private val _selectedRequest = MutableStateFlow<HelpRequest?>(null)
    val selectedRequest: StateFlow<HelpRequest?> = _selectedRequest.asStateFlow()

    val distanceFromUser: StateFlow<String> = combine(
        selectedRequest,
        userGeoPoint
    ) { request, userLocation ->
        if (request != null && userLocation != null) {
            try {
                val userPoint = Geometries.point(userLocation.latitude, userLocation.longitude)
                val requestPoint = Geometries.point(request.latitude, request.longitude)
                val distanceKm = userPoint.distance(requestPoint)
                String.format("%.1f km away", distanceKm)
            } catch (e: Exception) {
                "N/A"
            }
        } else {
            "..."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

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
            _respondUiState.value = if (success) {
                RespondUiState.Success
            } else {
                RespondUiState.Error("Failed to send offer.")
            }
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

    fun postRequest(
        title: String,
        description: String,
        category: String,
        compensation: String,
        locationName: String = ""  // Added default parameter
    ) {
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

            val lat = userProfile.roundedLat
            val lon = userProfile.roundedLon

            if (lat == null || lon == null) {
                _postRequestUiState.value = PostRequestUiState.Error("Please set your location in your profile first.")
                return@launch
            }

            val geohash = GeoHash.encodeHash(lat, lon, 7)
            val geohashCoarse = GeoHash.encodeHash(lat, lon, 5)

            val newRequest = HelpRequest(
                userId = currentUser.uid,
                userName = userProfile.name,
                title = title,
                description = description,
                category = category,
                locationName = locationName.ifEmpty { userProfile.area },
                type = if (compensation == "Fee") com.aidlink.model.RequestType.FEE else com.aidlink.model.RequestType.VOLUNTEER,
                status = "open",
                latitude = lat,
                longitude = lon,
                geohash = geohash,
                geohashCoarse = geohashCoarse
            )
            val success = repository.createRequest(newRequest)
            _postRequestUiState.value = if (success) PostRequestUiState.Success else PostRequestUiState.Error("Failed to post request.")
        }
    }

    fun resetPostRequestState() {
        _postRequestUiState.value = PostRequestUiState.Idle
    }
}

package com.aidlink.viewmodel

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aidlink.data.AuthRepository
import com.aidlink.data.MapboxGeocodingRepository
import com.aidlink.model.UserProfile
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val repository: AuthRepository,
    private val geocodingRepository: MapboxGeocodingRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    val searchResults = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    @SuppressLint("StaticFieldLeak")
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)
    private val _fcmToken = MutableStateFlow<String?>(null)
    private var imageUploadJob: Job? = null
    private var localImageUri: Uri? = null

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

    fun startProfileImageUpload(imageUri: Uri) {
        localImageUri = imageUri
        imageUploadJob?.cancel()
        val user = repository.getCurrentUser() ?: return
        imageUploadJob = viewModelScope.launch {
            repository.uploadProfileImage(user.uid, imageUri)
        }
        Log.d("AuthViewModel", "Started background profile image upload.")
    }
    
    fun searchAddress(query: String) {
        viewModelScope.launch {
            searchResults.value = geocodingRepository.forwardGeocode(query)
        }
    }

    fun sendOtp(activity: Activity, phoneNumber: String) {
        _uiState.value = AuthUiState.Loading
        val fullPhoneNumber = "+91$phoneNumber"
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

    @SuppressLint("MissingPermission")
    fun detectLocation(
        onResult: (areaString: String, geoPoint: GeoPoint, lat: Double, lon: Double, geohash: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val fused = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        val token = com.google.android.gms.tasks.CancellationTokenSource().token

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onError("Could not get current location. Try again outdoors.")
                    return@addOnSuccessListener
                }

                viewModelScope.launch {
                    try {
                        val address = geocodingRepository.reverseGeocode(location.latitude, location.longitude)
                            ?: "Unknown location"

                        val geoPoint = GeoPoint(location.latitude, location.longitude)
                        val geohash = com.github.davidmoten.geo.GeoHash.encodeHash(location.latitude, location.longitude, 7)

                        onResult(address, geoPoint, location.latitude, location.longitude, geohash)
                    } catch (e: Exception) {
                        onError("Reverse geocoding failed: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Location fetch failed", e)
                onError("Failed to get location: ${e.message}")
            }
    }


    fun saveUserProfile(
        name: String,
        bio: String,
        skills: List<String>,
        area: String,
        location: GeoPoint?,
        roundedLat: Double,
        roundedLon: Double,
        geohashCoarse: String
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val user = repository.getCurrentUser() ?: return@launch
            val userProfile = UserProfile(
                id = user.uid,
                name = name,
                bio = bio,
                skills = skills,
                area = area,
                photoUrl = "",
                fcmToken = _fcmToken.value ?: "",
                location = location,
                roundedLat = roundedLat,
                roundedLon = roundedLon,
                geohashCoarse = geohashCoarse,
                phone = user.phoneNumber ?: ""
            )

            val success = repository.createUserProfile(userProfile)
            if (!success) {
                setTemporaryError("Failed to save profile.")
                return@launch
            }
            _uiState.value = AuthUiState.AuthSuccessExistingUser

            val uploadJob = imageUploadJob
            val uri = localImageUri

            if (uploadJob != null && uri != null) {
                Log.d("AuthViewModel", "Waiting for background upload to finish...")

                uploadJob.join()
                val photoUrl = repository.uploadProfileImage(user.uid, uri)

                if (photoUrl != null) {
                    Log.d("AuthViewModel", "Background upload finished. Updating profile with URL.")
                    repository.updateProfilePhotoUrl(user.uid, photoUrl)
                } else {
                    Log.e("AuthViewModel", "Background upload finished but URL was null.")
                }
                imageUploadJob = null
                localImageUri = null
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

package com.aidlink

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aidlink.data.AuthRepository
import com.aidlink.ui.AppNavigation
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AppNavViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appNavViewModel: AppNavViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestLocationUpdates()
        } else {
            // TODO: Handle permission denial gracefully
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            requestLocationUpdates()
        }

        setContent {
            AidLinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    lifecycleScope.launch {
                        val user = authRepository.getCurrentUser()
                        if (user != null) {
                            authRepository.updateUserLocation(
                                user.uid,
                                GeoPoint(location.latitude, location.longitude)
                            )
                        }
                    }
                }
            }.addOnFailureListener {
                Log.e("MainActivity", "Failed to get location", it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val screen = it.getStringExtra("screen")
            if (screen == "review") {
                val requestId = it.getStringExtra("requestId")
                val revieweeId = it.getStringExtra("revieweeId")
                if (requestId != null && revieweeId != null) {
                    appNavViewModel.setReviewDeepLink(requestId, revieweeId)
                }
            } else {
                val chatId = it.getStringExtra("chatId")
                val userName = it.getStringExtra("userName") ?: it.getStringExtra("senderName")
                if (chatId != null && userName != null) {
                    appNavViewModel.setChatDeepLink(chatId, userName)
                }
            }
            it.removeExtra("screen")
            it.removeExtra("requestId")
            it.removeExtra("revieweeId")
            it.removeExtra("chatId")
            it.removeExtra("userName")
            it.removeExtra("senderName")
        }
    }
}
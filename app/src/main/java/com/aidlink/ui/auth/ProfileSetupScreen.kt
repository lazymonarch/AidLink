package com.aidlink.ui.auth

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.firestore.GeoPoint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel,
    onProfileSetupComplete: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    LaunchedEffect(key1 = uiState) {
        if (uiState is AuthUiState.AuthSuccessExistingUser) {
            onProfileSetupComplete()
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete your profile") },
                navigationIcon = {
                    IconButton(onClick = { /* Back navigation is handled by nav graph */ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = { galleryLauncher.launch("image/*") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add Photo",
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Text(
                text = "Add a profile photo",
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Full Name *", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = name, onValueChange = { name = it }, placeholder = "e.g., Jane Doe")

            Spacer(modifier = Modifier.height(24.dp))
            Text("Bio", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = bio, onValueChange = { bio = it }, placeholder = "Tell us about yourself", singleLine = false)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Skills *", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = skills, onValueChange = { skills = it }, placeholder = "e.g., gardening, tutoring")

            Spacer(modifier = Modifier.height(24.dp))
            Text("Area *", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = area,
                onValueChange = {
                    area = it
                    if (geoPoint != null) geoPoint = null
                },
                placeholder = { Text("e.g., sunnyvale, ca", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1C1C1E),
                    unfocusedContainerColor = Color(0xFF1C1C1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTrailingIconColor = Color.White,
                    unfocusedTrailingIconColor = Color.Gray
                ),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        if (locationPermissionState.status.isGranted) {
                            authViewModel.detectLocation(
                                onResult = { areaString, newGeoPoint ->
                                    area = areaString
                                    geoPoint = newGeoPoint
                                    Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Detect my location"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    authViewModel.saveUserProfile(name, bio, skillList, area, imageUri, geoPoint)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = name.isNotBlank() && skills.isNotBlank() && area.isNotBlank()
            ) {
                Text("Complete Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color.Gray) },
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1C1C1E),
            unfocusedContainerColor = Color(0xFF1C1C1E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        ProfileSetupScreen(authViewModel = viewModel(), onProfileSetupComplete = {})
    }
}
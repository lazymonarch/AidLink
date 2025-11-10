
package com.aidlink.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aidlink.viewmodel.EditProfileUiState
import com.aidlink.viewmodel.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    editProfileViewModel: EditProfileViewModel = hiltViewModel()
) {
    val userProfile by editProfileViewModel.userProfile.collectAsState()
    val uiState by editProfileViewModel.editProfileUiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )

    LaunchedEffect(userProfile) {
        userProfile?.let {
            name = it.name
            bio = it.bio
            skills = it.skills.joinToString(", ")
            area = it.area
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is EditProfileUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userProfile == null) {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(32.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri ?: userProfile?.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Change Picture", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(32.dp))

                ProfileTextField(label = "Full Name", value = name, onValueChange = { name = it })
                Spacer(Modifier.height(16.dp))
                ProfileTextField(label = "Short Bio", value = bio, onValueChange = { bio = it }, singleLine = false, modifier = Modifier.height(100.dp))
                Spacer(Modifier.height(16.dp))
                ProfileTextField(label = "Skills (comma-separated)", value = skills, onValueChange = { skills = it })
                Spacer(Modifier.height(16.dp))
                ProfileTextField(label = "Area (e.g., Sunnyvale, CA)", value = area, onValueChange = { area = it })

                Spacer(Modifier.weight(1f))

                if (uiState is EditProfileUiState.Error) {
                    Text((uiState as EditProfileUiState.Error).message, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { editProfileViewModel.onUpdateProfile(name, bio, skills, area, imageUri) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp),
                    shape = RoundedCornerShape(50),
                    enabled = uiState !is EditProfileUiState.Loading
                ) {
                    if (uiState is EditProfileUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
        )
    }
}


package com.aidlink.ui.auth

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import com.github.davidmoten.geo.GeoHash
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel,
    onProfileSetupComplete: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var roundedLat by remember { mutableStateOf(0.0) }
    var roundedLon by remember { mutableStateOf(0.0) }
    var geohashCoarse by remember { mutableStateOf("") }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        if (uri != null) {
            authViewModel.startProfileImageUpload(uri)
        }
    }

    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val finePermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val coarsePermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    LaunchedEffect(key1 = uiState) {
        if (uiState is AuthUiState.AuthSuccessExistingUser) {
            onProfileSetupComplete()
            authViewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Complete your profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentPage > 1) {
                        IconButton(onClick = { currentPage-- }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentPage < 3) {
                ProfileSetupBottomBar(
                    currentPage = currentPage,
                    onNext = { currentPage++ },
                    onPrev = { currentPage-- },
                    onSubmit = {
                        val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        authViewModel.saveUserProfile(name, bio, skillList, area, geoPoint, roundedLat, roundedLon, geohashCoarse)
                    },
                    isNextEnabled = name.isNotBlank(),
                    isSubmitEnabled = name.isNotBlank() && skills.isNotBlank() && area.isNotBlank(),
                    isLoading = uiState is AuthUiState.Loading
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                }
            }, label = "ProfileSetupPager"
        ) { page ->
            when (page) {
                1 -> Page1Content(
                    paddingValues = paddingValues,
                    name = name,
                    onNameChange = { name = it },
                    imageUri = imageUri,
                    onImageClick = { galleryLauncher.launch("image/*") }
                )
                2 -> Page2Content(
                    paddingValues = paddingValues,
                    bio = bio,
                    onBioChange = { bio = it },
                    skills = skills,
                    onSkillsChange = { skills = it }
                )
                3 -> Page3Content(
                    paddingValues = paddingValues,
                    area = area,
                    authViewModel = authViewModel,
                    onAreaChange = {
                        area = it
                        if (geoPoint != null) geoPoint = null
                    },
                    onDetectLocationClick = {
                        if (finePermission.status.isGranted || coarsePermission.status.isGranted) {
                            authViewModel.detectLocation(
                                onResult = { areaString, newGeoPoint, rLat, rLon, gCoarse ->
                                    area = areaString
                                    geoPoint = newGeoPoint
                                    roundedLat = rLat
                                    roundedLon = rLon
                                    geohashCoarse = gCoarse
                                    Toast.makeText(context, "Location set!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            finePermission.launchPermissionRequest()
                        }
                    },
                    onLocationSelected = { lat, lon ->
                        geoPoint = GeoPoint(lat, lon)
                        roundedLat = lat
                        roundedLon = lon
                        geohashCoarse = GeoHash.encodeHash(lat, lon, 5)
                        Toast.makeText(context, "Location set from search!", Toast.LENGTH_SHORT).show()
                    },
                    onPrev = { currentPage-- },
                    onSubmit = {
                        val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        authViewModel.saveUserProfile(name, bio, skillList, area, geoPoint, roundedLat, roundedLon, geohashCoarse)
                    },
                    isSubmitEnabled = name.isNotBlank() && skills.isNotBlank() && area.isNotBlank(),
                    isLoading = uiState is AuthUiState.Loading
                )
            }
        }
    }
}

@Composable
private fun Page1Content(
    paddingValues: PaddingValues,
    name: String,
    onNameChange: (String) -> Unit,
    imageUri: Uri?,
    onImageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
        progress = { 0.33f },
        modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
        color = ProgressIndicatorDefaults.linearColor,
        trackColor = ProgressIndicatorDefaults.linearTrackColor,
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable { onImageClick() }
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Add photo",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Add Photo",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "(Optional)",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Full Name *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("e.g., Jane Doe", color = Color(0xFFCAC4D0))
            },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Page2Content(
    paddingValues: PaddingValues,
    bio: String,
    onBioChange: (String) -> Unit,
    skills: String,
    onSkillsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LinearProgressIndicator(
            progress = 0.66f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Bio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = {
                if (it.length <= 150) onBioChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text("Tell us about yourself", color = Color(0xFFCAC4D0))
            },
            supportingText = {
                Text(
                    "${bio.length}/150",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Skills *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "Separate skills with a comma",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = skills,
            onValueChange = onSkillsChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("e.g., gardening, tutoring, plumbing", color = Color(0xFFCAC4D0))
            },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            )
        )

        if (skills.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.split(",").forEach { skill ->
                    if (skill.trim().isNotBlank()) {
                        AssistChip(
                            onClick = { },
                            label = { Text(skill.trim()) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page3Content(
    paddingValues: PaddingValues,
    area: String,
    authViewModel: AuthViewModel,
    onAreaChange: (String) -> Unit,
    onDetectLocationClick: () -> Unit,
    onLocationSelected: ((Double, Double) -> Unit)? = null,
    onPrev: () -> Unit,
    onSubmit: () -> Unit,
    isSubmitEnabled: Boolean,
    isLoading: Boolean
) {
    var showSearchBottomSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by authViewModel.searchResults.collectAsState()

    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { query ->
                if (query.isNotBlank()) {
                    authViewModel.searchAddress(query)
                }
            }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp)
    ) {
        LinearProgressIndicator(
            progress = 1.0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Color(0xFFFF6B35),
            trackColor = Color(0xFFFFE5DD)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Area *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1B1F)
        )

        Text(
            text = "This helps us find requests near you.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF79747E),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = area,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSearchBottomSheet = true },
            enabled = false,
            placeholder = {
                Text(
                    "Search for your area",
                    color = Color(0xFFCAC4D0)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF49454F)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { onDetectLocationClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Use current location",
                        tint = Color(0xFFFF6B35)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFCAC4D0),
                disabledContainerColor = Color.White,
                disabledTextColor = Color(0xFF1C1B1F),
                disabledPlaceholderColor = Color(0xFFCAC4D0),
                disabledLeadingIconColor = Color(0xFF49454F),
                disabledTrailingIconColor = Color(0xFFFF6B35)
            )
        )

        if (area.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFFE5DD).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Selected area",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF79747E)
                    )
                    Text(
                        text = area,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1C1B1F)
                    )
                }
                IconButton(
                    onClick = { onAreaChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFF6B35)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFFF6B35)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Previous",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isSubmitEnabled && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFCAC4D0),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 3.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        "Complete",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showSearchBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSearchBottomSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCAC4D0)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Search for your area",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Type area name",
                            color = Color(0xFFCAC4D0)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF49454F)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF49454F)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedContainerColor = Color(0xFFFAFAFA),
                        unfocusedContainerColor = Color(0xFFFAFAFA),
                        cursorColor = Color(0xFFFF6B35)
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults.size) { index ->
                        val result = searchResults[index]
                        ListItem(
                            headlineContent = { Text(result.first) },
                            supportingContent = { Text(result.second) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF49454F)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAreaChange(result.first)
                                    if (onLocationSelected != null) {
                                        onLocationSelected(0.0, 0.0) // Dummy coordinates
                                    }
                                    showSearchBottomSheet = false
                                }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileSetupBottomBar(
    currentPage: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit,
    isNextEnabled: Boolean,
    isSubmitEnabled: Boolean,
    isLoading: Boolean
) {
    Surface(tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 1) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Previous",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Placeholder
            }

            if (currentPage < 3) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isNextEnabled && !isLoading,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = isSubmitEnabled && !isLoading,
                    modifier = Modifier.height(56.dp).weight(1f),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Complete", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    AidLinkTheme {
        ProfileSetupScreen(authViewModel = viewModel(), onProfileSetupComplete = {})
    }
}

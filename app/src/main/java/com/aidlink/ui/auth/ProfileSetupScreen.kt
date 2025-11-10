
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
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
import com.aidlink.R
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
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
                    if (currentPage > 1) {
                        IconButton(onClick = { currentPage-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ProfileSetupBottomBar(
                currentPage = currentPage,
                onNext = { currentPage++ },
                onPrev = { currentPage-- },
                onSubmit = {
                    val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    authViewModel.saveUserProfile(name, bio, skillList, area, geoPoint, roundedLat, roundedLon, geohashCoarse)
                },
                isNextEnabled = if (currentPage == 1) name.isNotBlank() else true,
                isSubmitEnabled = name.isNotBlank() && skills.isNotBlank() && area.isNotBlank(),
                isLoading = uiState is AuthUiState.Loading
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->

        val scrollState = rememberScrollState()

        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
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
            Column {
                when (page) {
                    1 -> Page1Content(
                        name = name,
                        onNameChange = { name = it },
                        imageUri = imageUri,
                        onImageClick = { galleryLauncher.launch("image/*") }
                    )
                    2 -> Page2Content(
                        bio = bio,
                        onBioChange = { bio = it },
                        skills = skills,
                        onSkillsChange = { skills = it }
                    )
                    3 -> Page3Content(
                        area = area,
                        onAreaChange = {
                            area = it
                            if (geoPoint != null) geoPoint = null
                        },
                        onDetectLocationClick = {
                            if (locationPermissionState.status.isGranted) {
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
                                locationPermissionState.launchPermissionRequest()
                            }
                        },
                        onLocationSelected = { lat, lon ->
                            // Handle coordinates from Mapbox search
                            geoPoint = GeoPoint(lat, lon)
                            roundedLat = lat
                            roundedLon = lon
                            geohashCoarse = GeoHash.encodeHash(lat, lon, 5)
                            Toast.makeText(context, "Location set from search!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Page1Content(
    name: String,
    onNameChange: (String) -> Unit,
    imageUri: Uri?,
    onImageClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onImageClick
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
            text = "Add a profile photo (Optional)",
            color = Color.Gray,
            modifier = Modifier
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Full Name *", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = name, onValueChange = onNameChange, placeholder = "e.g., Jane Doe")
    }
}

@Composable
private fun Page2Content(
    bio: String,
    onBioChange: (String) -> Unit,
    skills: String,
    onSkillsChange: (String) -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Bio", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = bio, onValueChange = onBioChange, placeholder = "Tell us about yourself", singleLine = false, modifier = Modifier.height(100.dp))

        Spacer(modifier = Modifier.height(24.dp))
        Text("Skills *", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Text("Separate skills with a comma", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
        CustomTextField(value = skills, onValueChange = onSkillsChange, placeholder = "e.g., gardening, tutoring, plumbing")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Page3Content(
    area: String,
    onAreaChange: (String) -> Unit,
    onDetectLocationClick: () -> Unit,
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val searchEngine = remember {
        com.mapbox.search.SearchEngine.createSearchEngineWithBuiltInDataProviders(
            com.mapbox.search.ApiType.SEARCH_BOX,
            com.mapbox.search.SearchEngineSettings()
        )
    }
    var showSearchSheet by remember { mutableStateOf(false) }

    if (showSearchSheet) {
        MapboxSearchBottomSheet(
            searchEngine = searchEngine,
            onDismiss = { showSearchSheet = false },
            onPlaceSelected = { placeSelection ->
                onAreaChange(placeSelection.name)
                onLocationSelected?.invoke(placeSelection.latitude, placeSelection.longitude)
                showSearchSheet = false
            }
        )
    }

    Column {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Area *", color = Color.White, style = MaterialTheme.typography.labelLarge)
        Text("This helps us find requests near you.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = area,
            onValueChange = { /* Ignore manual input; rely on autocomplete */ },
            placeholder = { Text("Search for your area", color = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSearchSheet = true },
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
            readOnly = true,
            trailingIcon = {
                Row {
                    IconButton(onClick = onDetectLocationClick) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Detect my location"
                        )
                    }
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search area"
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapboxSearchBottomSheet(
    searchEngine: com.mapbox.search.SearchEngine,
    onDismiss: () -> Unit,
    onPlaceSelected: (PlaceSelection) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<com.mapbox.search.result.SearchSuggestion>()) }
    var searchTask by remember { mutableStateOf<com.mapbox.search.common.AsyncOperationTask?>(null) }

    val searchCallback = remember {
        object : com.mapbox.search.SearchSelectionCallback {
            override fun onSuggestions(
                suggestionsList: List<com.mapbox.search.result.SearchSuggestion>,
                responseInfo: com.mapbox.search.ResponseInfo
            ) {
                suggestions = suggestionsList
            }

            override fun onResult(
                suggestion: com.mapbox.search.result.SearchSuggestion,
                result: com.mapbox.search.result.SearchResult,
                responseInfo: com.mapbox.search.ResponseInfo
            ) {
                val coordinate = result.coordinate
                val lat = coordinate?.latitude() ?: 0.0
                val lon = coordinate?.longitude() ?: 0.0
                val placeName = result.name
                
                android.util.Log.d("MapboxSearch", "Selected place: $placeName at lat=$lat, lon=$lon")
                onPlaceSelected(PlaceSelection(placeName, lat, lon))
            }

            override fun onResults(
                suggestion: com.mapbox.search.result.SearchSuggestion,
                results: List<com.mapbox.search.result.SearchResult>,
                responseInfo: com.mapbox.search.ResponseInfo
            ) {
                // Handle category search results if needed
            }

            override fun onError(e: Exception) {
                android.util.Log.e("MapboxSearch", "Search error", e)
            }
        }
    }

    LaunchedEffect(query) {
        searchTask?.cancel()
        if (query.length >= 2) {
            val searchOptions = com.mapbox.search.SearchOptions.Builder()
                .limit(5)
                .build()
            searchTask = searchEngine.search(query, searchOptions, searchCallback)
        } else {
            suggestions = emptyList()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            searchTask?.cancel()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1C1C1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 400.dp)
        ) {
            Text("Search for your area", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Type area name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2C2C2E),
                    unfocusedContainerColor = Color(0xFF2C2C2E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchTask = searchEngine.select(suggestion, searchCallback)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(suggestion.name, color = Color.White, fontSize = 16.sp)
                            suggestion.address?.formattedAddress()?.let { address ->
                                Text(address, color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class PlaceSelection(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

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
    Surface(
        color = Color.Black,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentPage > 1) {
                TextButton(onClick = onPrev, enabled = !isLoading) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // Placeholder
            }

            if (currentPage < 3) {
                Button(
                    onClick = onNext,
                    enabled = isNextEnabled && !isLoading
                ) {
                    Text("Next")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            } else {
                Button(
                    onClick = onSubmit,
                    enabled = isSubmitEnabled && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text("Complete Profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
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

package com.aidlink.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel
import com.aidlink.viewmodel.PostRequestUiState
import androidx.compose.material3.ExposedDropdownMenuDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    homeViewModel: HomeViewModel,
    onClose: () -> Unit,
    onPostRequestSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }

    val categories = listOf(
        CategoryItem("Home & Garden", Icons.Default.Home),
        CategoryItem("Moving & Delivery", Icons.Default.LocalShipping),
        CategoryItem("Tutoring & Education", Icons.Default.School),
        CategoryItem("Tech Support", Icons.Default.Computer),
        CategoryItem("Pet Care", Icons.Default.Pets),
        CategoryItem("Errands", Icons.AutoMirrored.Filled.DirectionsRun),
        CategoryItem("Other", Icons.Default.MoreHoriz)
    )

    var selectedCategory by remember { mutableStateOf(categories[0].name) }
    var selectedCompensation by remember { mutableStateOf("Fee") }
    val postRequestUiState by homeViewModel.postRequestUiState.collectAsState()

    // Form validation
    val isTitleValid = title.length >= 10
    val isDescriptionValid = description.length >= 20
    val isLocationValid = locationName.isNotEmpty()
    val isFormValid = isTitleValid && isDescriptionValid && isLocationValid

    // Calculate completion percentage
    val completionPercentage = remember(title, description, locationName, selectedCategory) {
        var completed = 0
        if (title.length >= 10) completed += 25
        if (description.length >= 20) completed += 25
        if (locationName.isNotEmpty()) completed += 25
        if (selectedCategory.isNotEmpty()) completed += 25
        completed
    }

    LaunchedEffect(key1 = postRequestUiState) {
        if (postRequestUiState is PostRequestUiState.Success) {
            onPostRequestSuccess()
            homeViewModel.resetPostRequestState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "New Request",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { /* Save as draft */ },
                        enabled = title.isNotEmpty() || description.isNotEmpty()
                    ) {
                        Text(
                            "Save Draft",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress Indicator Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fill in the details",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completionPercentage% Complete",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { completionPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }

            // Form Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Title Field
                PostFormTextField(
                    label = "Title",
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    placeholder = "e.g., Need help assembling a bookshelf",
                    isRequired = true,
                    isError = title.isNotEmpty() && !isTitleValid,
                    supportingText = when {
                        title.isEmpty() -> "Required field"
                        !isTitleValid -> "Title should be at least 10 characters"
                        else -> "${title.length}/100"
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )

                // Description Field
                PostFormTextField(
                    label = "Description",
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    placeholder = "Provide more details about what you need help with...",
                    isRequired = true,
                    isError = description.isNotEmpty() && !isDescriptionValid,
                    supportingText = when {
                        description.isEmpty() -> "Required field"
                        !isDescriptionValid -> "Add more details (min 20 characters)"
                        else -> "${description.length}/500"
                    },
                    singleLine = false,
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.height(140.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )

                // Category Dropdown
                PostCategoryDropdown(
                    items = categories,
                    selectedItem = selectedCategory,
                    onItemSelected = { selectedCategory = it }
                )

                // Location Field
                PostLocationField(
                    location = locationName,
                    onLocationClick = {
                        // TODO: Open location picker
                        locationName = "Current Location" // Placeholder
                    },
                    onUseCurrentLocation = {
                        // TODO: Get current location
                        locationName = "Current Location"
                    }
                )

                // Compensation Type
                PostCompensationToggle(
                    selectedOption = selectedCompensation,
                    onOptionSelected = { selectedCompensation = it }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = {
                            homeViewModel.postRequest(
                                title,
                                description,
                                selectedCategory,
                                selectedCompensation,
                                locationName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isFormValid && postRequestUiState !is PostRequestUiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (postRequestUiState is PostRequestUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Post Request",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Helper text
                    AnimatedVisibility(
                        visible = !isFormValid,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Please fill all required fields correctly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data class for categories
data class CategoryItem(
    val name: String,
    val icon: ImageVector
)

@Composable
private fun PostFormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isError: Boolean = false,
    supportingText: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (isRequired) "$label *" else label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = isError,
            supportingText = if (supportingText.isNotEmpty()) {
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            supportingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else null,
            keyboardOptions = keyboardOptions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCategoryDropdown(
    items: List<CategoryItem>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Category *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                leadingIcon = {
                    items.find { it.name == selectedItem }?.let { category ->
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                items.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = if (selectedItem == category.name)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        },
                        onClick = {
                            onItemSelected(category.name)
                            isExpanded = false
                        },
                        leadingIcon = if (selectedItem == category.name) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun PostLocationField(
    location: String,
    onLocationClick: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Location *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = location,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // Added rememberRipple()
                    onClick = onLocationClick
                ),
            enabled = false,
            placeholder = {
                Text(
                    "Tap to set location",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (location.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                IconButton(onClick = onUseCurrentLocation) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Use current location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PostCompensationToggle(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Compensation *",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Fee Button
            FilterChip(
                selected = selectedOption == "Fee",
                onClick = { onOptionSelected("Fee") },
                label = {
                    Text(
                        "Paid Help",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedOption == "Fee",
                    borderColor = if (selectedOption == "Fee")
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    borderWidth = 1.5.dp
                )
            )

            // Volunteer Button
            FilterChip(
                selected = selectedOption == "Volunteer",
                onClick = { onOptionSelected("Volunteer") },
                label = {
                    Text(
                        "Volunteer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onTertiary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedOption == "Volunteer",
                    borderColor = if (selectedOption == "Volunteer")
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.outline,
                    borderWidth = 1.5.dp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostRequestScreenPreview() {
    AidLinkTheme {
        PostRequestScreen(
            homeViewModel = hiltViewModel(),
            onClose = {},
            onPostRequestSuccess = {}
        )
    }
}
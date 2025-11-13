package com.aidlink.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aidlink.model.RequestType
import com.aidlink.viewmodel.EditRequestViewModel
import kotlinx.coroutines.flow.collectLatest

// Category data class
data class Category(val name: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

val categories = listOf(
    Category("Home & Garden", Icons.Default.Home),
    Category("Moving & Delivery", Icons.Default.LocalShipping),
    Category("Tech Support", Icons.Default.Computer),
    Category("Pet Care", Icons.Default.Pets),
    Category("Other", Icons.Default.Category)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    onNavigateBack: () -> Unit,
    viewModel: EditRequestViewModel = hiltViewModel()
) {
    // State collection
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val compensationType by viewModel.compensationType.collectAsState()
    val location by viewModel.location.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    // Dialog state
    var showExitDialog by remember { mutableStateOf(false) }
    
    // Handle back press when there are unsaved changes
    BackHandler(enabled = hasUnsavedChanges) {
        showExitDialog = true
    }

    // Listen for save result
    LaunchedEffect(Unit) {
        viewModel.saveResult.collectLatest { success ->
            if (success) {
                onNavigateBack()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Request",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            if (hasUnsavedChanges) {
                                showExitDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = Color(0xFF1C1B1F)
                        )
                    }
                },
                actions = {
                    // Reset button - only show when there are unsaved changes
                    if (hasUnsavedChanges) {
                        TextButton(
                            onClick = { viewModel.resetChanges() }
                        ) {
                            Text(
                                "Reset",
                                color = Color(0xFFB3261E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Unsaved changes banner
            if (hasUnsavedChanges) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF3E0)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFB8C00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You have unsaved changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFB8C00)
                        )
                    }
                }
            }
            
            // Form content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title field
                FormSection(
                    label = "Title",
                    isRequired = true
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { viewModel.updateTitle(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Brief summary of what you need", color = Color(0xFFCAC4D0)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B35),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color(0xFFFF6B35)
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Description field
                FormSection(
                    label = "Description",
                    isRequired = true
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { 
                            if (it.length <= 500) viewModel.updateDescription(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { 
                            Text(
                                "Provide details about the help you need...",
                                color = Color(0xFFCAC4D0)
                            ) 
                        },
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "${description.length}/500",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (description.length > 450) 
                                        Color(0xFFFF6B35) 
                                    else 
                                        Color(0xFF79747E)
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B35),
                            unfocusedBorderColor = Color(0xFFCAC4D0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Color(0xFFFF6B35)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 6
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category dropdown
                FormSection(
                    label = "Category",
                    isRequired = true
                ) {
                    CategoryDropdown(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { viewModel.selectCategory(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Location field
                FormSection(
                    label = "Location",
                    isRequired = true
                ) {
                    LocationField(
                        location = location,
                        onLocationClick = { /* Open location picker */ },
                        onCurrentLocationClick = { /* Use current location */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Compensation type
                FormSection(
                    label = "Compensation",
                    isRequired = true
                ) {
                    CompensationSelector(
                        selectedType = compensationType,
                        onTypeSelected = { viewModel.setCompensationType(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Bottom action bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = { viewModel.saveChanges() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = hasUnsavedChanges && !isSaving && 
                                 title.isNotBlank() && description.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B35),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCAC4D0),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 8.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFB8C00),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Unsaved Changes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "You have unsaved changes. Are you sure you want to leave without saving?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF49454F)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(
                        "Discard",
                        color = Color(0xFFB3261E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(
                        "Keep Editing",
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun FormSection(
    label: String,
    isRequired: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1B1F)
            )
            if (isRequired) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFB3261E)
                )
            }
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = showMenu,
        onExpandedChange = { showMenu = it }
    ) {
        OutlinedTextField(
            value = selectedCategory,
            onValueChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (showMenu)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF49454F)
                )
            },
            leadingIcon = {
                val selectedCategoryIcon = categories.find { it.name == selectedCategory }?.icon
                selectedCategoryIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF6B35),
                unfocusedBorderColor = Color(0xFFCAC4D0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        
        ExposedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color.White)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.name)
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = Color(0xFF49454F),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = if (selectedCategory == category.name) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFFFF6B35)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun LocationField(
    location: String,
    onLocationClick: () -> Unit,
    onCurrentLocationClick: () -> Unit
) {
    OutlinedTextField(
        value = location,
        onValueChange = { },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLocationClick),
        enabled = false,
        placeholder = {
            Text(
                "Tap to set location",
                color = Color(0xFFCAC4D0)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF6B35)
            )
        },
        trailingIcon = {
            IconButton(onClick = onCurrentLocationClick) {
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
            disabledLeadingIconColor = Color(0xFFFF6B35),
            disabledTrailingIconColor = Color(0xFFFF6B35)
        )
    )
}

@Composable
private fun CompensationSelector(
    selectedType: RequestType,
    onTypeSelected: (RequestType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Paid Help option
        FilterChip(
            selected = selectedType == RequestType.FEE,
            onClick = { onTypeSelected(RequestType.FEE) },
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
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
                containerColor = Color.White,
                labelColor = Color(0xFF49454F)
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedType == RequestType.FEE,
                borderColor = if (selectedType == RequestType.FEE)
                    Color.Transparent
                else
                    Color(0xFFCAC4D0),
                borderWidth = 1.5.dp
            )
        )
        
        // Volunteer option
        FilterChip(
            selected = selectedType == RequestType.VOLUNTEER,
            onClick = { onTypeSelected(RequestType.VOLUNTEER) },
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
                selectedContainerColor = Color(0xFF00897B),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
                containerColor = Color.White,
                labelColor = Color(0xFF49454F)
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedType == RequestType.VOLUNTEER,
                borderColor = if (selectedType == RequestType.VOLUNTEER)
                    Color.Transparent
                else
                    Color(0xFFCAC4D0),
                borderWidth = 1.5.dp
            )
        )
    }
}
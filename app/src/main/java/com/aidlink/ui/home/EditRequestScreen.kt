package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidlink.viewmodel.EditRequestUiState
import com.aidlink.viewmodel.EditRequestViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRequestScreen(
    onNavigateBack: () -> Unit,
    editRequestViewModel: EditRequestViewModel = hiltViewModel()
) {
    val request by editRequestViewModel.request.collectAsState()
    val uiState by editRequestViewModel.editRequestUiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Home & Garden", "Moving & Delivery", "Tech Support", "Pet Care", "Other")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedCompensation by remember { mutableStateOf("Fee") }

    // This effect will run once when the request data is loaded from the ViewModel,
    // pre-filling the input fields with the existing data.
    LaunchedEffect(request) {
        request?.let {
            title = it.title
            description = it.description
            selectedCategory = it.category
            selectedCompensation = it.type.name.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is EditRequestUiState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Request", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF131313),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF131313),
                border = BorderStroke(1.dp, Color(0xFF3D4F53))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState is EditRequestUiState.Loading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Button(
                            onClick = {
                                editRequestViewModel.onUpdateRequest(title, description, selectedCategory, selectedCompensation)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            enabled = title.isNotBlank() && description.isNotBlank()
                        ) {
                            Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF131313)
    ) { innerPadding ->
        if (request == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Re-using the same input composable from PostRequestScreen would be ideal here
                // For now, we define them locally for clarity.
                FormInput(label = "Title", value = title, onValueChange = { title = it })
                FormInput(label = "Description", value = description, onValueChange = { description = it }, singleLine = false, modifier = Modifier.height(120.dp))
                CategoryDropdown(label = "Category", items = categories, selectedItem = selectedCategory, onItemSelected = { selectedCategory = it })
                CompensationToggle(label = "Compensation", selectedOption = selectedCompensation, onOptionSelected = { selectedCompensation = it })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// NOTE: These are helper composable, identical to the ones in PostRequestScreen.
// In a real project, you would move these to a common file to avoid code duplication.
@Composable
private fun FormInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF182C30),
                unfocusedContainerColor = Color(0xFF182C30),
                cursorColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color(0xFF3D4F53)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(label: String, items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
            OutlinedTextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF182C30),
                    unfocusedContainerColor = Color(0xFF182C30),
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF3D4F53),
                    focusedTrailingIconColor = Color.Gray,
                    unfocusedTrailingIconColor = Color.Gray
                )
            )
            ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }, modifier = Modifier.background(Color(0xFF182C30))) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(item, color = Color.White) }, onClick = { onItemSelected(item); isExpanded = false })
                }
            }
        }
    }
}

@Composable
private fun CompensationToggle(label: String, selectedOption: String, onOptionSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Fee", "Volunteer").forEach { option ->
                val isSelected = selectedOption.equals(option, ignoreCase = true)
                OutlinedButton(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(width = 2.dp, color = if (isSelected) Color.White else Color(0xFF3D4F53)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = if (isSelected) Color.White else Color.Gray
                    )
                ) {
                    Text(option, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
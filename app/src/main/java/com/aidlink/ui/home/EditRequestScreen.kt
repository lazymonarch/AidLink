
package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState is EditRequestUiState.Loading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = {
                                editRequestViewModel.onUpdateRequest(title, description, selectedCategory, selectedCompensation)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = title.isNotBlank() && description.isNotBlank()
                        ) {
                            Text("Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
                EditFormInput(label = "Title", value = title, onValueChange = { title = it })
                EditFormInput(label = "Description", value = description, onValueChange = { description = it }, singleLine = false, modifier = Modifier.height(120.dp))
                EditCategoryDropdown(items = categories, selectedItem = selectedCategory, onItemSelected = { selectedCategory = it })
                EditCompensationToggle(selectedOption = selectedCompensation, onOptionSelected = { selectedCompensation = it })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EditFormInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDropdown(items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { isExpanded = it }) {
            OutlinedTextField(
                value = selectedItem,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { onItemSelected(item); isExpanded = false })
                }
            }
        }
    }
}

@Composable
private fun EditCompensationToggle(selectedOption: String, onOptionSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Compensation", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Fee", "Volunteer").forEach { option ->
                val isSelected = selectedOption.equals(option, ignoreCase = true)
                val colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }

                OutlinedButton(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                    colors = colors
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(option, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

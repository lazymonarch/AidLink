
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel
import com.aidlink.viewmodel.PostRequestUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    homeViewModel: HomeViewModel,
    onClose: () -> Unit,
    onPostRequestSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Home & Garden", "Moving & Delivery", "Tech Support", "Pet Care", "Other")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var selectedCompensation by remember { mutableStateOf("Fee") }
    val postRequestUiState by homeViewModel.postRequestUiState.collectAsState()

    LaunchedEffect(key1 = postRequestUiState) {
        if (postRequestUiState is PostRequestUiState.Success) {
            onPostRequestSuccess()
            homeViewModel.resetPostRequestState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Request", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (postRequestUiState) {
                        is PostRequestUiState.Loading -> {
                            CircularProgressIndicator()
                        }
                        else -> {
                            Button(
                                onClick = { homeViewModel.postRequest(title, description, selectedCategory, selectedCompensation) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = title.isNotBlank() && description.isNotBlank()
                            ) {
                                Text("Post Request", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PostFormInput(
                label = "Title",
                value = title,
                onValueChange = { title = it },
                placeholder = "e.g., Need help assembling a bookshelf"
            )

            PostFormInput(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                placeholder = "Provide more details about the task...",
                singleLine = false,
                modifier = Modifier.height(120.dp)
            )

            PostCategoryDropdown(
                items = categories,
                selectedItem = selectedCategory,
                onItemSelected = { selectedCategory = it }
            )

            PostCompensationToggle(
                selectedOption = selectedCompensation,
                onOptionSelected = { selectedCompensation = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PostFormInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCategoryDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                shape = RoundedCornerShape(12.dp),
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            onItemSelected(item)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCompensationToggle(
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Compensation", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Fee", "Volunteer").forEach { option ->
                val isSelected = selectedOption == option
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
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
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

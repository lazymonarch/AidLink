package com.aidlink.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.HomeViewModel
import com.aidlink.viewmodel.RequestUiState

private val backgroundDark = Color(0xFF131313)
private val textDark = Color(0xFFF6F8F8)
private val textDark80 = Color(0xFFF6F8F8).copy(alpha = 0.8f)
private val surfaceDark = Color(0xFF182C30)
private val outlineDark = Color(0xFF3D4F53)
private val placeholderDark = Color(0xFFA0B8BC)
private val primaryWhite = Color(0xFFFFFFFF)

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

    val requestUiState by homeViewModel.requestUiState.collectAsState()

    LaunchedEffect(key1 = requestUiState) {
        if (requestUiState is RequestUiState.Success) {
            onPostRequestSuccess()
            homeViewModel.resetRequestState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Request", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundDark,
                    titleContentColor = textDark,
                    navigationIconContentColor = textDark
                )
            )
        },
        bottomBar = {
            Surface(
                color = backgroundDark,
                border = BorderStroke(1.dp, outlineDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (requestUiState) {
                        is RequestUiState.Loading -> {
                            CircularProgressIndicator(color = Color.White)
                        }
                        else -> {
                            Button(
                                onClick = { homeViewModel.postRequest(title, description, selectedCategory, selectedCompensation) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryWhite,
                                    contentColor = Color.Black
                                ),
                                enabled = title.isNotBlank() && description.isNotBlank()
                            ) {
                                Text("Post Request", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = backgroundDark
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

            FormInput(
                label = "Title",
                value = title,
                onValueChange = { title = it }, // <-- CORRECTED
                placeholder = "e.g., Need help assembling a bookshelf"
            )

            FormInput(
                label = "Description",
                value = description,
                onValueChange = { description = it }, // <-- CORRECTED
                placeholder = "Provide more details about the task...",
                singleLine = false,
                modifier = Modifier.height(120.dp)
            )

            CategoryDropdown(
                label = "Category",
                items = categories,
                selectedItem = selectedCategory,
                onItemSelected = { selectedCategory = it }
            )

            CompensationToggle(
                label = "Compensation",
                selectedOption = selectedCompensation,
                onOptionSelected = { selectedCompensation = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = textDark80, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textDark,
                unfocusedTextColor = textDark,
                focusedContainerColor = surfaceDark,
                unfocusedContainerColor = surfaceDark,
                cursorColor = primaryWhite,
                focusedBorderColor = primaryWhite,
                unfocusedBorderColor = outlineDark,
                focusedPlaceholderColor = placeholderDark,
                unfocusedPlaceholderColor = placeholderDark
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = textDark80, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textDark,
                    unfocusedTextColor = textDark,
                    focusedContainerColor = surfaceDark,
                    unfocusedContainerColor = surfaceDark,
                    cursorColor = primaryWhite,
                    focusedBorderColor = primaryWhite,
                    unfocusedBorderColor = outlineDark,
                    focusedTrailingIconColor = placeholderDark,
                    unfocusedTrailingIconColor = placeholderDark
                )
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier.background(surfaceDark)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, color = textDark) },
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
private fun CompensationToggle(
    label: String,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = textDark80, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf("Fee", "Volunteer").forEach { option ->
                val isSelected = selectedOption == option
                OutlinedButton(
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isSelected) primaryWhite else outlineDark
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) primaryWhite.copy(alpha = 0.1f) else Color.Transparent,
                        contentColor = if (isSelected) primaryWhite else placeholderDark
                    )
                ) {
                    Text(option, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PostRequestScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        PostRequestScreen(
            homeViewModel = viewModel(),
            onClose = {},
            onPostRequestSuccess = {}
        )
    }
}
package com.aidlink.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel,
    onProfileSetupComplete: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

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
            Text("Full Name", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = name, onValueChange = { name = it }, placeholder = "e.g., Jane Doe")
            Spacer(modifier = Modifier.height(24.dp))
            Text("Skills", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = skills, onValueChange = { skills = it }, placeholder = "e.g., gardening, tutoring")
            Spacer(modifier = Modifier.height(24.dp))
            Text("Area", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(value = area, onValueChange = { area = it }, placeholder = "e.g., sunnyvale, ca")
            Spacer(modifier = Modifier.height(24.dp))
            Text("Short Bio", color = Color.White, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            CustomTextField(
                value = bio,
                onValueChange = { bio = it },
                placeholder = "Tell us a little bit about yourself",
                modifier = Modifier.height(120.dp),
                singleLine = false
            )
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val skillList = skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    authViewModel.saveUserProfile(name, skillList, area, bio)
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
        ProfileSetupScreen(authViewModel = AuthViewModel(), onProfileSetupComplete = {})
    }
}
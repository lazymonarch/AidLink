package com.aidlink.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.viewmodel.DeleteAccountUiState
import com.aidlink.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val uiState by settingsViewModel.deleteAccountUiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is DeleteAccountUiState.Success) {
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
            settingsViewModel.resetState()
        }
    }

    if (showConfirmDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                settingsViewModel.onDeleteAccountClicked()
            },
            isLoading = uiState is DeleteAccountUiState.Loading
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        ) {
            HorizontalDivider(color = Color.DarkGray)

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                text = "Delete Account",
                color = Color.Red,
                onClick = { showConfirmDialog = true }
            )
            HorizontalDivider(color = Color.DarkGray)
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    text: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = color, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to permanently delete your account? All of your data, including your requests and profile, will be erased. This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
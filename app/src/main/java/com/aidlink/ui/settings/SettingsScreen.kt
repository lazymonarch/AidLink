
package com.aidlink.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.aidlink.data.AuthRepository
import com.aidlink.viewmodel.DeleteAccountUiState
import com.aidlink.viewmodel.SettingsViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showLogOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val deleteAccountState by viewModel.deleteAccountUiState.collectAsState()
    
    // Handle delete account success
    LaunchedEffect(deleteAccountState) {
        if (deleteAccountState is DeleteAccountUiState.Success) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            viewModel.resetState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Account Section
            item {
                SettingsSectionHeader(
                    title = "Account",
                    icon = Icons.Default.Person
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = { navController.navigate("edit_profile") }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Account",
                    subtitle = "Permanently delete your account",
                    onClick = { showDeleteDialog = true },
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Notifications Section
            item {
                SettingsSectionHeader(
                    title = "Notifications",
                    icon = Icons.Default.Notifications
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Push Notifications",
                    subtitle = "Manage push notification preferences",
                    onClick = { /* TODO: Navigate to push notifications */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Email,
                    title = "Email Notifications",
                    subtitle = "Manage email notification preferences",
                    onClick = { /* TODO: Navigate to email notifications */ }
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Privacy & Safety Section
            item {
                SettingsSectionHeader(
                    title = "Privacy & Safety",
                    icon = Icons.Default.Lock
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Blocked Users",
                    subtitle = "Manage blocked users",
                    onClick = { /* TODO: Navigate to blocked users */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Visibility,
                    title = "Profile Visibility",
                    subtitle = "Control who can see your profile",
                    onClick = { /* TODO: Navigate to profile visibility */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Shield,
                    title = "Data & Privacy",
                    subtitle = "Manage your data and privacy settings",
                    onClick = { /* TODO: Navigate to data & privacy */ }
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // App Preferences Section
            item {
                SettingsSectionHeader(
                    title = "Preferences",
                    icon = Icons.Default.Palette
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = "Light, Dark, or System default",
                    onClick = { /* TODO: Navigate to theme settings */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English (US)",
                    onClick = { /* TODO: Navigate to language settings */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Straighten,
                    title = "Distance Units",
                    subtitle = "Kilometers",
                    onClick = { /* TODO: Navigate to distance units */ }
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Help & Support Section
            item {
                SettingsSectionHeader(
                    title = "Help & Support",
                    icon = Icons.Default.Help
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.HelpCenter,
                    title = "Help Center",
                    subtitle = "Browse help articles and FAQs",
                    onClick = { /* TODO: Navigate to help center */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.ContactSupport,
                    title = "Contact Support",
                    subtitle = "Get help from our support team",
                    onClick = { /* TODO: Navigate to contact support */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Report,
                    title = "Report a Problem",
                    subtitle = "Let us know about issues you're experiencing",
                    onClick = { /* TODO: Navigate to report problem */ }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "Terms & Privacy Policy",
                    subtitle = "Read our terms and privacy policy",
                    onClick = { /* TODO: Navigate to terms */ }
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // About Section
            item {
                SettingsSectionHeader(
                    title = "About",
                    icon = Icons.Default.Info
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.0.0 (Build 1)",
                    onClick = { },
                    showTrailingIcon = false
                )
            }
            
            item {
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Log Out Section
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogOutDialog = true },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Log Out Confirmation Dialog
    if (showLogOutDialog) {
        AlertDialog(
            onDismissRequest = { showLogOutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Log Out?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to log out of your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogOutDialog = false
                        viewModel.logOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        "Log Out",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogOutDialog = false }) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.onDeleteAccountClicked()
            },
            isLoading = deleteAccountState is DeleteAccountUiState.Loading,
            error = (deleteAccountState as? DeleteAccountUiState.Error)?.message
        )
    }
}

// Settings Section Header
@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

// Settings Item
@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showTrailingIcon: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Trailing arrow
            if (showTrailingIcon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

// Delete Account Confirmation Dialog
@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Delete Account?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to permanently delete your account? All of your data, including your requests and profile, will be erased. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Delete",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

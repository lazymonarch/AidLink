package com.aidlink.ui.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.R
import com.aidlink.ui.common.ThreeDOrbitLoader
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel
import com.aidlink.ui.theme.Montserrat

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToOtp: (String) -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = uiState) {
        if (uiState is AuthUiState.OtpSent) {
            onNavigateToOtp((uiState as AuthUiState.OtpSent).verificationId)
        }
    }

    // Wrap the entire screen in a Box to allow overlaying the loader
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- THIS IS THE UPDATED LOGO ---
            Image(
                painter = painterResource(id = R.drawable.aidlink_logo1),
                contentDescription = "AidLink Logo",
                modifier = Modifier.fillMaxWidth(1f),
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "When hearts gettogether",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontFamily = Montserrat,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Enter your phone number to get started.",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                prefix = { Text("+91 ", color = Color.White) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.DarkGray,
                    unfocusedContainerColor = Color.DarkGray,
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- THIS IS THE FIX ---
            // This Box ensures the space is always occupied, even if the button is hidden.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                // Only show the button if the state is NOT loading.
                if (uiState !is AuthUiState.Loading) {
                    Button(
                        onClick = { authViewModel.sendOtp("+91$phoneNumber", activity) },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        enabled = phoneNumber.isNotBlank()
                    ) {
                        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Show the overlay and loader only when the state is Loading
        if (uiState is AuthUiState.Loading) {
            // Semi-transparent overlay for visual polish
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            // Your custom loader, positioned in the bottom half of the screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ThreeDOrbitLoader()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewDark() {
    AidLinkTheme(darkTheme = true) {
        LoginScreen(authViewModel = AuthViewModel(), onNavigateToOtp = {})
    }
}
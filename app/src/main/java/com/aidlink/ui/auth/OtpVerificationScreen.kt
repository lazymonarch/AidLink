package com.aidlink.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.ui.common.ThreeDOrbitLoader // <-- Import your loader
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    authViewModel: AuthViewModel,
    verificationId: String,
    onBackClicked: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = uiState) {
        when (uiState) {
            is AuthUiState.AuthSuccessExistingUser -> {
                onNavigateToHome()
                authViewModel.resetState()
            }
            is AuthUiState.AuthSuccessNewUser -> {
                onNavigateToProfileSetup()
                authViewModel.resetState()
            }
            else -> { /* Do nothing */ }
        }
    }

    // Wrap the screen in a Box to allow overlaying the loader
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make TopAppBar transparent
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent // Make Scaffold transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Enter verification code",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We've sent a 6-digit code to your phone.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    OtpInputField(
                        otpText = otpValue,
                        onOtpTextChange = { value, _ ->
                            if (value.length <= 6) { otpValue = value }
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    Row {
                        Text("Didn't receive a code? ", color = Color.Gray)
                        Text(
                            text = "Resend",
                            color = Color(0xFFE57373),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState is AuthUiState.Error) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    } else if (uiState !is AuthUiState.Loading) {
                        // --- THIS IS THE FIX ---
                        // Only show the button if the state is NOT loading.
                        Button(
                            onClick = { authViewModel.verifyOtp(verificationId, otpValue) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            enabled = otpValue.length == 6
                        ) {
                            Text("Verify", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Show the overlay and loader only when the state is Loading
        if (uiState is AuthUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
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

@Composable
private fun OtpInputField(
    modifier: Modifier = Modifier,
    otpText: String,
    otpCount: Int = 6,
    onOtpTextChange: (String, Boolean) -> Unit
) {
    BasicTextField(
        modifier = modifier,
        value = otpText,
        onValueChange = {
            if (it.length <= otpCount) {
                onOtpTextChange.invoke(it, it.length == otpCount)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(otpCount) { index ->
                    val char = otpText.getOrNull(index)?.toString() ?: ""
                    val isFocused = otpText.length == index
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(55.dp)
                            .padding(horizontal = 4.dp)
                            .border(
                                1.dp,
                                if (isFocused) Color.LightGray else Color.Gray,
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun OtpVerificationScreenPreview() {
    AidLinkTheme(darkTheme = true) {
        OtpVerificationScreen(
            authViewModel = AuthViewModel(),
            verificationId = "test_id",
            onBackClicked = {},
            onNavigateToHome = {},
            onNavigateToProfileSetup = {}
        )
    }
}
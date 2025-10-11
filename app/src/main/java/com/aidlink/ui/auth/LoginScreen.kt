package com.aidlink.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AID LINK",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When hearts get together",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is AuthUiState.Loading -> {
                    CircularProgressIndicator(color = Color.White)
                }
                else -> {
                    Button(
                        onClick = { authViewModel.sendOtp("+91$phoneNumber", activity) },
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewDark() {
    AidLinkTheme(darkTheme = true) {
        LoginScreen(
            authViewModel = AuthViewModel(),
            onNavigateToOtp = {}
        )
    }
}
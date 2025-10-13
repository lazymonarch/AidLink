package com.aidlink.ui.auth

import android.annotation.SuppressLint
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.R
import com.aidlink.ui.common.SphereLoader
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.ui.theme.Montserrat
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel

@SuppressLint("ContextCastToActivity")
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToOtp: (String) -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val phoneNumber by authViewModel.phoneNumber.collectAsState()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(key1 = uiState) {
        if (uiState is AuthUiState.OtpSent) {
            onNavigateToOtp((uiState as AuthUiState.OtpSent).verificationId)
            authViewModel.resetState()
        }
    }

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
                onValueChange = { authViewModel.onPhoneNumberChanged(it) },
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
                    .height(56.dp)
            ) {
                if (uiState is AuthUiState.Idle || uiState is AuthUiState.Error) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                authViewModel.sendOtp(activity)
                            }
                        },
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

        if (uiState is AuthUiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SphereLoader()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewDark() {
    AidLinkTheme(darkTheme = true) {
        LoginScreen(authViewModel = viewModel(), onNavigateToOtp = {})
    }
}
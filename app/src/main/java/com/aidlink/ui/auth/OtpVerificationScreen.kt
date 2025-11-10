
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidlink.ui.theme.AidLinkTheme
import com.aidlink.viewmodel.AuthUiState
import com.aidlink.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    authViewModel: AuthViewModel,
    verificationId: String,
    onBackClicked: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    val uiState by authViewModel.uiState.collectAsState()

    var timeLeft by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = timeLeft, key2 = canResend) {
        if (!canResend) {
            delay(1000)
            timeLeft--
            if (timeLeft == 0) {
                canResend = true
            }
        }
    }

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
            is AuthUiState.Error -> {
                otpDigits = List(6) { "" }
            }
            else -> { /* Do nothing */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                )
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Enter verification code",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We've sent a 6-digit code to your phone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(40.dp))

                OtpInput(
                    otpDigits = otpDigits,
                    onOtpDigitsChange = { otpDigits = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Didn't receive a code? ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            timeLeft = 60
                            canResend = false
                            // TODO: Resend OTP logic
                        },
                        enabled = canResend
                    ) {
                        Text(
                            text = if (canResend) "Resend" else "Resend in 0:${timeLeft.toString().padStart(2, '0')}",
                            fontWeight = FontWeight.SemiBold,
                            color = if (canResend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { authViewModel.verifyOtp(verificationId, otpDigits.joinToString("")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = otpDigits.all { it.isNotEmpty() } && uiState !is AuthUiState.Loading,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Text(
                        text = "Verify",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (uiState is AuthUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun OtpInput(
    otpDigits: List<String>,
    onOtpDigitsChange: (List<String>) -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        otpDigits.forEachIndexed { index, digit ->
            val isFilled = digit.isNotEmpty()

            BasicTextField(
                value = digit,
                onValueChange = {
                    val newDigits = otpDigits.toMutableList()
                    if (it.length <= 1) {
                        newDigits[index] = it
                        onOtpDigitsChange(newDigits)
                        if (it.isNotEmpty() && index < 5) {
                            focusRequesters[index + 1].requestFocus()
                        }
                    } else if (it.length == 6) {
                        onOtpDigitsChange(it.chunked(1))
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent {
                        if (it.key == Key.Backspace && digit.isEmpty() && index > 0) {
                            focusRequesters[index - 1].requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (isFilled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isFilled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (digit.isEmpty()) {
                            Text(
                                text = "-",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            innerTextField()
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OtpVerificationScreenPreview() {
    AidLinkTheme {
        OtpVerificationScreen(
            authViewModel = viewModel(),
            verificationId = "test_id",
            onBackClicked = {},
            onNavigateToHome = {},
            onNavigateToProfileSetup = {}
        )
    }
}

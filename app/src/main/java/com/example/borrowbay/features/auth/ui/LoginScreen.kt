package com.example.borrowbay.features.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.features.auth.viewmodel.AuthState
import com.example.borrowbay.features.auth.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showPhoneInput by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                snackbarHostState.showSnackbar((authState as AuthState.Success).message)
            }
            is AuthState.Authenticated -> {
                onLoginSuccess()
            }
            is AuthState.Error -> {
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = "Welcome to BorrowBay",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A202C)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Create an account to start renting and earning.",
                fontSize = 18.sp,
                color = Color(0xFF718096)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (authState is AuthState.OtpSent) {
                // OTP Entry View
                Text("Enter OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A202C))
                Text("Sent to ${(authState as AuthState.OtpSent).phoneNumber}", fontSize = 14.sp, color = Color(0xFF718096))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("000000", color = Color(0xFF718096)) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.verifyOtp((authState as AuthState.OtpSent).phoneNumber, otpCode) },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                    enabled = otpCode.length >= 6 && authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Verify OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = { viewModel.resetState() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Phone Number", color = Color(0xFF0066FF))
                }
            } else {
                // Main Login View
                AnimatedVisibility(visible = !showPhoneInput) {
                    Column {
                        OutlinedButton(
                            onClick = { viewModel.signInWithGoogle() },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A202C))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G", fontWeight = FontWeight.Bold, color = Color(0xFFEA4335), fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showPhoneInput = true },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A202C))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Continue with Phone", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text("or", modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFF718096))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text("Email address", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A202C))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("you@example.com", color = Color(0xFF718096)) },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.signInWithEmail(email) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                            enabled = email.isNotEmpty() && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Continue with Email", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showPhoneInput) {
                    Column {
                        Text("Phone Number", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A202C))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("+1234567890", color = Color(0xFF718096)) },
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.signInWithPhone(phoneNumber) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF)),
                            enabled = phoneNumber.isNotEmpty() && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Send OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { showPhoneInput = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Back to other options", color = Color(0xFF718096))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF38A169)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your data is encrypted and secure", fontSize = 14.sp, color = Color(0xFF718096))
            }
        }
    }
}

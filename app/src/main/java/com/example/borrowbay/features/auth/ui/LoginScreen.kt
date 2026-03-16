package com.example.borrowbay.features.auth.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.core.ui.components.PhoneInputField
import com.example.borrowbay.core.ui.components.countries
import com.example.borrowbay.features.auth.viewmodel.AuthState
import com.example.borrowbay.features.auth.viewmodel.AuthViewModel
import com.example.borrowbay.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNeedsRegistration: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(countries.first()) }
    var otpCode by remember { mutableStateOf("") }
    var showPhoneInput by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    BackHandler(enabled = showPhoneInput || authState is AuthState.OtpSent) {
        if (authState is AuthState.OtpSent) {
            viewModel.resetState()
        } else if (showPhoneInput) {
            showPhoneInput = false
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> onLoginSuccess()
            is AuthState.NeedsRegistration -> onNeedsRegistration()
            is AuthState.Error -> {
                snackbarHostState.showSnackbar((authState as AuthState.Error).message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = if (isSignUpMode) "Create Account" else "Welcome to BorrowBay",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (isSignUpMode) 
                    "Join our community and start earning." 
                    else "Rent anything, anywhere, anytime.",
                fontSize = 16.sp,
                color = MutedFgLight
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (authState is AuthState.OtpSent) {
                Text("Enter OTP", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                Text("Sent to ${(authState as AuthState.OtpSent).phoneNumber}", fontSize = 14.sp, color = MutedFgLight)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) otpCode = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("000000", color = MutedFgLight) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ocean,
                        unfocusedBorderColor = BorderLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedContainerColor = SurfaceLight,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.verifyOtp(otpCode) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                    enabled = otpCode.length >= 6 && authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Verify OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = { viewModel.resetState() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Phone Number", color = Ocean)
                }
            } else {
                AnimatedVisibility(visible = !showPhoneInput) {
                    Column {
                        OutlinedButton(
                            onClick = { showPhoneInput = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceLight, contentColor = Color.Black)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(22.dp), tint = Ocean)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    if (isSignUpMode) "Sign up with Phone" else "Continue with Phone",
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                            Text("or", modifier = Modifier.padding(horizontal = 16.dp), color = MutedFgLight)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text("Email address", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.replace("\n", "") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("you@example.com", color = MutedFgLight) },
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Ocean,
                                unfocusedBorderColor = BorderLight,
                                unfocusedContainerColor = SurfaceLight,
                                focusedContainerColor = SurfaceLight,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Password", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.replace("\n", "") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Minimum 6 characters", color = MutedFgLight) },
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = MutedFgLight)
                                }
                            },
                            singleLine = true,
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Ocean,
                                unfocusedBorderColor = BorderLight,
                                unfocusedContainerColor = SurfaceLight,
                                focusedContainerColor = SurfaceLight,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { 
                                if (isSignUpMode) {
                                    viewModel.signUpWithEmail(email, password)
                                } else {
                                    viewModel.signInWithEmail(email, password)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                            enabled = email.isNotEmpty() && password.length >= 6 && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (isSignUpMode) "Create Account" else "Continue with Email", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                                color = MutedFgLight
                            )
                            TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                                Text(
                                    text = if (isSignUpMode) "Sign In" else "Sign Up",
                                    color = Ocean,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = showPhoneInput) {
                    Column {
                        PhoneInputField(
                            phoneNumber = phoneNumber,
                            onPhoneNumberChange = { phoneNumber = it },
                            selectedCountry = selectedCountry,
                            onCountrySelected = { selectedCountry = it }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { 
                                val fullPhone = selectedCountry.code + phoneNumber
                                viewModel.signInWithPhone(fullPhone, context as Activity) 
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                            enabled = phoneNumber.length == selectedCountry.maxLength && authState !is AuthState.Loading
                        ) {
                            if (authState is AuthState.Loading) {
                                CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Send OTP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = { showPhoneInput = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Back to other options", color = MutedFgLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Emerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Your data is encrypted and secure", fontSize = 14.sp, color = MutedFgLight)
            }
        }
    }
}

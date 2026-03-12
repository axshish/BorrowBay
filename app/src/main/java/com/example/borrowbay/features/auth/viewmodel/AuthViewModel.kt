package com.example.borrowbay.features.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.borrowbay.core.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signInWithEmail(emailAddress: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(
                    provider = Email,
                    redirectUrl = "borrowbay://login"
                ) {
                    email = emailAddress
                }
                _authState.value = AuthState.Success("Check your email for the login link!")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(
                    provider = Google,
                    redirectUrl = "borrowbay://login"
                )
                _authState.value = AuthState.Success("Redirecting to Google...")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun signInWithPhone(phoneNumber: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                supabase.auth.signInWith(OTP) {
                    phone = phoneNumber
                }
                _authState.value = AuthState.OtpSent(phoneNumber)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun verifyOtp(phoneNumber: String, otpCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // In Supabase v3, verifying phone OTP uses verifyPhoneOtp
                supabase.auth.verifyPhoneOtp(
                    type = OtpType.Phone.SMS,
                    phone = phoneNumber,
                    token = otpCode
                )
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid OTP")
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class OtpSent(val phoneNumber: String) : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

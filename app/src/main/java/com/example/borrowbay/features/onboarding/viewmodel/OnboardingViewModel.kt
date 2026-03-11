package com.example.borrowbay.features.onboarding.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.borrowbay.features.onboarding.model.OnboardingPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {
    private val _pages = MutableStateFlow(
        listOf(
            OnboardingPage(
                title = "Rent anything nearby",
                description = "Discover cameras, tools, bikes and more — available from people around you.",
                icon = Icons.Default.LocationOn,
                iconColor = Color(0xFF0066FF),
                buttonText = "Continue"
            ),
            OnboardingPage(
                title = "Earn from unused items",
                description = "Turn idle possessions into income. List in minutes, earn while you sleep.",
                icon = Icons.Default.AttachMoney,
                iconColor = Color(0xFF00C853),
                buttonText = "Continue"
            ),
            OnboardingPage(
                title = "Escrow-protected rentals",
                description = "Deposits held securely. Automatically refunded when items return safely.",
                icon = Icons.Default.Security,
                iconColor = Color(0xFFFFB300),
                buttonText = "Get Started"
            )
        )
    )
    val pages: StateFlow<List<OnboardingPage>> = _pages.asStateFlow()
}

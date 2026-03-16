package com.example.borrowbay.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.borrowbay.core.ui.components.PhoneInputField
import com.example.borrowbay.core.ui.components.countries
import com.example.borrowbay.features.profile.model.UserProfile
import com.example.borrowbay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    
    // Parse phone and country from profile.phone (which includes the code)
    val initialCountry = remember(profile.phone) {
        countries.find { profile.phone.startsWith(it.code) } ?: countries.first()
    }
    val initialPhone = remember(profile.phone, initialCountry) {
        profile.phone.removePrefix(initialCountry.code)
    }
    
    var phone by remember { mutableStateOf(initialPhone) }
    var selectedCountry by remember { mutableStateOf(initialCountry) }
    
    var email by remember { mutableStateOf(profile.email) }
    var address by remember { mutableStateOf(profile.address) }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceLight,
                        shadowElevation = 1.dp,
                        modifier = Modifier.padding(start = 16.dp).size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = Color.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Full Name", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("\n", "") },
                placeholder = { Text("Enter your name", color = MutedFgLight) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

            PhoneInputField(
                phoneNumber = phone,
                onPhoneNumberChange = { phone = it },
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it },
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Email Address", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.replace("\n", "") },
                placeholder = { Text("Enter your email", color = MutedFgLight) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
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

            Text("Address", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text("Enter your address", color = MutedFgLight) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp),
                minLines = 4,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ocean,
                    unfocusedBorderColor = BorderLight,
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val fullPhone = if (phone.isNotBlank()) selectedCountry.code + phone else ""
                    onSave(profile.copy(name = name, phone = fullPhone, email = email, address = address))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                enabled = phone.length == selectedCountry.maxLength || phone.isBlank()
            ) {
                Text("Update Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

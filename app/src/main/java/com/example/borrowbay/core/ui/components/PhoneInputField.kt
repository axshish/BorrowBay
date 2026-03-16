package com.example.borrowbay.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.window.Dialog
import com.example.borrowbay.ui.theme.*

data class Country(
    val name: String,
    val code: String,
    val flag: String,
    val maxLength: Int
)

val countries = listOf(
    Country("India", "+91", "🇮🇳", 10),
    Country("United States", "+1", "🇺🇸", 10),
    Country("United Kingdom", "+44", "🇬🇧", 10),
    Country("Australia", "+61", "🇦🇺", 9),
    Country("Canada", "+1", "🇨🇦", 10),
    Country("Germany", "+49", "🇩🇪", 11),
    Country("France", "+33", "🇫🇷", 9),
    Country("Japan", "+81", "🇯🇵", 10),
    Country("Singapore", "+65", "🇸🇬", 8),
    Country("United Arab Emirates", "+971", "🇦🇪", 9)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputField(
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    label: String = "Phone Number",
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { 
                if (it.length <= selectedCountry.maxLength && it.all { char -> char.isDigit() }) {
                    onPhoneNumberChange(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter phone number", color = MutedFgLight) },
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
            keyboardActions = keyboardActions,
            singleLine = true,
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .clickable { showCountryPicker = true }
                        .padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedCountry.flag, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = selectedCountry.code,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Country",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .height(24.dp)
                            .width(1.dp)
                            .background(BorderLight)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ocean,
                unfocusedBorderColor = BorderLight,
                unfocusedContainerColor = SurfaceLight,
                focusedContainerColor = SurfaceLight,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )
        
        Text(
            text = "Limit: ${phoneNumber.length}/${selectedCountry.maxLength} digits",
            fontSize = 11.sp,
            color = if (phoneNumber.length == selectedCountry.maxLength) Ocean else MutedFgLight,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }

    if (showCountryPicker) {
        CountryCodePickerDialog(
            onDismiss = { showCountryPicker = false },
            onCountrySelected = {
                onCountrySelected(it)
                showCountryPicker = false
            }
        )
    }
}

@Composable
fun CountryCodePickerDialog(
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            countries
        } else {
            countries.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.code.contains(searchQuery) 
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Country",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search country or code") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ocean,
                        unfocusedBorderColor = BorderLight
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(filteredCountries) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = country.flag, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = country.name,
                                modifier = Modifier.weight(1f),
                                color = Color.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = country.code,
                                color = Ocean,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        HorizontalDivider(color = BorderLight)
                    }
                }
            }
        }
    }
}

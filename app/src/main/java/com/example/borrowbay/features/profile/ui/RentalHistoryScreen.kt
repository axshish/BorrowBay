package com.example.borrowbay.features.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.features.home.ui.components.RentalCard
import com.example.borrowbay.features.profile.viewmodel.ProfileViewModel
import com.example.borrowbay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalHistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val rentals by viewModel.userRentals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Rentals", fontWeight = FontWeight.Bold, color = Color.Black) },
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
        Box(modifier = modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Ocean)
            } else if (rentals.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No rentals yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Items you rent will appear here.", fontSize = 14.sp, color = MutedFgLight)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(rentals) { item ->
                        RentalCard(
                            item = item, 
                            showRenterInfo = true,
                            showRentalStatus = true
                        )
                    }
                }
            }
        }
    }
}

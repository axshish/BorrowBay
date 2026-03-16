package com.example.borrowbay.features.profile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.features.home.ui.components.RentalCard
import com.example.borrowbay.features.profile.viewmodel.ProfileViewModel
import com.example.borrowbay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveListingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val listings by viewModel.userListings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("My Listings", fontWeight = FontWeight.Bold, color = Color.Black) },
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
            } else if (listings.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No listings yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Items you publish will appear here.", fontSize = 14.sp, color = MutedFgLight)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(listings) { item ->
                        SellerListingItem(
                            item = item,
                            onReturnClick = { viewModel.markAsReturned(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SellerListingItem(
    item: RentalItem,
    onReturnClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.5f))
    ) {
        Column {
            RentalCard(
                item = item,
                showRentalStatus = true,
                modifier = Modifier.padding(bottom = 0.dp)
            )
            
            if (!item.isAvailable) {
                Button(
                    onClick = onReturnClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Available", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

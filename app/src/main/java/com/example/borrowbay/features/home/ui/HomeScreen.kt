package com.example.borrowbay.features.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.borrowbay.features.home.ui.components.*
import com.example.borrowbay.features.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(onProfileClick = onProfileClick)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.offset(y = 50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 1. Header
                    item {
                        HomeHeader(
                            location = uiState.userLocation,
                            onFilterClick = { /* TODO */ }
                        )
                    }

                    // 2. Category Row
                    item {
                        CategoryRow(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategory,
                            onCategorySelected = { viewModel.onCategorySelected(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 3. Nearby Rentals Section
                    item {
                        SectionHeader(
                            title = "Nearby rentals",
                            onSeeAllClick = { /* TODO */ }
                        )
                    }

                    // Grid layout for Nearby
                    val nearbyRows = uiState.nearbyRentals.chunked(2)
                    items(nearbyRows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                RentalCard(
                                    item = item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { /* TODO */ }
                                )
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // 4. Trending Section (Matching Reference Image)
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        SectionHeader(
                            title = "Trending now",
                            onSeeAllClick = { /* TODO */ }
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.trendingRentals) { item ->
                                RentalCard(
                                    item = item,
                                    modifier = Modifier.width(240.dp),
                                    onClick = { /* TODO */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            BottomNavItem("Home", Icons.Default.Home, onHomeClick, true),
            BottomNavItem("Add", Icons.Default.Add, onAddClick, false, isFab = true),
            BottomNavItem("Profile", Icons.Default.PersonOutline, onProfileClick, false)
        )

        items.forEach { item ->
            if (item.isFab) {
                NavigationBarItem(
                    selected = false,
                    onClick = item.onClick,
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = { Text(item.label) }
                )
            } else {
                NavigationBarItem(
                    selected = item.isSelected,
                    onClick = item.onClick,
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) }
                )
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isSelected: Boolean,
    val isFab: Boolean = false
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

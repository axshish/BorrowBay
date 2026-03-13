@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.borrowbay.features.home.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.borrowbay.data.model.Category
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.features.home.ui.components.RentalCard
import com.example.borrowbay.features.home.viewmodel.HomeViewModel
import com.example.borrowbay.features.home.viewmodel.SortOption
import com.example.borrowbay.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMoreGlobal()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            HomeTopBar(
                address = uiState.userAddress,
                onLocationClick = { viewModel.toggleLocationPicker(true) }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = {},
                onAddClick = onAddClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.onRefresh() },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Razorpay Missing Warning
                if (uiState.userRazorpayId.isNullOrBlank()) {
                    item {
                        RazorpayWarningBanner(
                            onRegisterClick = { viewModel.showRazorpaySetup(true) }
                        )
                    }
                }

                // Search and Filters
                item {
                    SearchAndFiltersSection(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        selectedSort = uiState.selectedSort,
                        onSortChange = { viewModel.onSortOptionSelected(it) }
                    )
                }

                // Categories
                item {
                    CategorySection(
                        categories = uiState.categories,
                        selectedId = uiState.selectedCategory,
                        onCategorySelect = { viewModel.onCategorySelected(it) }
                    )
                }

                // Nearby Grid
                if (uiState.isLoading && uiState.nearbyRentals.isEmpty()) {
                    item { SectionHeader("Nearby rentals", "Finding items near you...") }
                    items(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(Modifier.weight(1f).height(200.dp).clip(RoundedCornerShape(20.dp)).shimmerLoadingAnimation())
                            Box(Modifier.weight(1f).height(200.dp).clip(RoundedCornerShape(20.dp)).shimmerLoadingAnimation())
                        }
                    }
                } else if (uiState.nearbyRentals.isNotEmpty()) {
                    item {
                        SectionHeader("Nearby rentals", "Within 10km radius")
                    }
                    val rows = uiState.nearbyRentals.chunked(2)
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { item ->
                                RentalCard(
                                    item = item, 
                                    modifier = Modifier.weight(1f),
                                    onClick = { onItemClick(item.id) }
                                )
                            }
                            if (rowItems.size < 2) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // Global List
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("All items", "From everywhere")
                }

                if (uiState.isLoading && uiState.globalRentals.isEmpty()) {
                    items(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .shimmerLoadingAnimation()
                        )
                    }
                } else if (uiState.globalRentals.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No items found in marketplace.", color = Color.Gray)
                        }
                    }
                } else {
                    items(uiState.globalRentals) { item ->
                        GlobalRentalItem(
                            item = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Ocean)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (uiState.isLocationPickerVisible) {
        LocationPickerDialog(
            initialLat = uiState.userLatitude,
            initialLng = uiState.userLongitude,
            onDismiss = { viewModel.toggleLocationPicker(false) },
            onLocationSelected = { lat, lng, addr -> 
                viewModel.updateLocation(lat, lng, addr)
            }
        )
    }

    if (uiState.isRazorpaySetupVisible) {
        RazorpaySetupDialog(
            onDismiss = { viewModel.showRazorpaySetup(false) },
            onSave = { id -> viewModel.saveRazorpayId(id) }
        )
    }
}

private fun simplifyAddress(address: String): String {
    val parts = address.split(",")
    return if (parts.size >= 3) {
        "${parts[parts.size - 3].trim()}, ${parts[parts.size - 2].trim()}"
    } else {
        address
    }
}

@Composable
fun RazorpayWarningBanner(onRegisterClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF3E0),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFE65100))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Payments not setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFE65100)
                )
                Text(
                    "You need a Razorpay ID to receive payments.",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100)
                )
            }
            Text(
                "Setup",
                modifier = Modifier.clickable { onRegisterClick() }.padding(8.dp),
                color = Ocean,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@Composable
fun RazorpaySetupDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var razorpayId by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Razorpay") },
        text = {
            Column {
                Text("Enter your Razorpay Account ID (acc_...) to receive payments from lenders.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = razorpayId,
                    onValueChange = { razorpayId = it },
                    placeholder = { Text("acc_Hxxxxxx") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Don't have one? Use 'acc_TEST_123' for testing.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (razorpayId.isNotBlank()) onSave(razorpayId) },
                colors = ButtonDefaults.buttonColors(containerColor = Ocean)
            ) {
                Text("Save ID")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HomeTopBar(address: String, onLocationClick: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp).clickable { onLocationClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = Ocean, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Location", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Ocean)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Ocean, modifier = Modifier.size(16.dp))
                }
                Text(address, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Black)
            }
        }
    }
}

@Composable
fun SearchAndFiltersSection(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedSort: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search cameras, tools, etc.") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Ocean,
                    unfocusedBorderColor = BorderLight
                )
            )
            Spacer(Modifier.width(12.dp))
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.size(52.dp).background(MutedLight, RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Tune, null, tint = Color.Black)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            },
                            trailingIcon = { if (selectedSort == option) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySection(categories: List<Category>, selectedId: String?, onCategorySelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { category ->
            val isSelected = selectedId == category.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { category.icon?.let { Text(it) } },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Ocean,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
        Text(subtitle, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun GlobalRentalItem(item: RentalItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(simplifyAddress(item.location), fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${item.pricePerDay.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ocean)
                    Text("/day", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(color = Ocean.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("${String.format(Locale.ROOT, "%.1f", item.distance)}km", color = Ocean, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
            BottomNavItem("Profile", Icons.Default.Person, onProfileClick, false)
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

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isSelected: Boolean,
    val isFab: Boolean = false
)

@Composable
fun LocationPickerDialog(
    initialLat: Double?,
    initialLng: Double?,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var selectedLat by remember { mutableDoubleStateOf(initialLat ?: 0.0) }
    var selectedLng by remember { mutableDoubleStateOf(initialLng ?: 0.0) }
    var currentAddress by remember { mutableStateOf("") }
    
    var suggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun updateFromLocation(lat: Double, lng: Double) {
        selectedLat = lat
        selectedLng = lng
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addressList = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(lat, lng, 1)
                }
                val addr = addressList?.firstOrNull()
                if (addr != null) {
                    currentAddress = addr.getAddressLine(0) ?: ""
                }
            } catch (e: Exception) {
                Log.e("LocationPicker", "Error fetching address", e)
            }
        }
    }

    fun searchAddress(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch(Dispatchers.IO) {
            delay(500)
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 5)
                withContext(Dispatchers.Main) {
                    suggestions = results ?: emptyList()
                    showSuggestions = suggestions.isNotEmpty()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val requestLocation = {
        try {
            @SuppressLint("MissingPermission")
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        updateFromLocation(loc.latitude, loc.longitude)
                    }
                }
        } catch (e: SecurityException) {
            Log.e("LocationPicker", "Location permission missing", e)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            requestLocation()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Set Location") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = { 
                        TextButton(
                            onClick = { onLocationSelected(selectedLat, selectedLng, currentAddress); onDismiss() },
                            enabled = selectedLat != 0.0
                        ) { 
                            Text("Confirm", color = Ocean, fontWeight = FontWeight.Bold) 
                        } 
                    }
                )

                Box(modifier = Modifier.padding(16.dp)) {
                    Column {
                        OutlinedTextField(
                            value = currentAddress,
                            onValueChange = {
                                currentAddress = it
                                if (it.length > 3) searchAddress(it) else showSuggestions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search address...") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Ocean) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fineGranted) requestLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }) {
                                    Icon(Icons.Outlined.MyLocation, null, tint = Ocean)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (showSuggestions) {
                            DropdownMenu(
                                expanded = showSuggestions,
                                onDismissRequest = { showSuggestions = false },
                                modifier = Modifier.fillMaxWidth(0.9f),
                                properties = PopupProperties(focusable = false)
                            ) {
                                suggestions.forEach { addr ->
                                    DropdownMenuItem(
                                        text = { Text(addr.getAddressLine(0)) },
                                        onClick = {
                                            showSuggestions = false
                                            updateFromLocation(addr.latitude, addr.longitude)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(15.0)
                                val receiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        updateFromLocation(p.latitude, p.longitude)
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                }
                                overlays.add(MapEventsOverlay(receiver))
                            }
                        },
                        update = { view ->
                            if (selectedLat != 0.0) {
                                val geoPoint = GeoPoint(selectedLat, selectedLng)
                                view.controller.animateTo(geoPoint)
                                view.overlays.removeAll { it is Marker }
                                val marker = Marker(view).apply {
                                    position = geoPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Selected Location"
                                }
                                view.overlays.add(marker)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

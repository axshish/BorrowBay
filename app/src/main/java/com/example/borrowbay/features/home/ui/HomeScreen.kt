@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.borrowbay.features.home.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import org.osmdroid.config.Configuration
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
    val context = LocalContext.current

    // Permission Launcher for initial location detection
    val initialPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.detectCurrentLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasFine || hasCoarse) {
            viewModel.detectCurrentLocation(context)
        } else {
            initialPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

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
        containerColor = BackgroundLight,
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = { },
                onAddClick = onAddClick,
                onProfileClick = onProfileClick,
                userAvatarUrl = uiState.userAvatarUrl
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
                // Location Bar
                item {
                    HomeTopBar(
                        address = uiState.userAddress,
                        onLocationClick = { viewModel.toggleLocationPicker(true) }
                    )
                }

                // Search Bar and Filter
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

                // Nearby Rentals
                if (uiState.isLoading && uiState.nearbyRentals.isEmpty()) {
                    item { SectionHeader("Nearby rentals", "Finding items near you...") }
                    items(2) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(Modifier.weight(1f).height(200.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceLight))
                            Box(Modifier.weight(1f).height(200.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceLight))
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

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SectionHeader("Explore marketplace", "From everywhere")
                }

                // Global Marketplace
                if (uiState.globalRentals.isNotEmpty()) {
                    items(uiState.globalRentals) { item ->
                        GlobalRentalItem(
                            item = item,
                            onClick = { onItemClick(item.id) }
                        )
                    }
                } else if (!uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No items found in marketplace.", color = MutedFgLight)
                        }
                    }
                }

                if (uiState.isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Ocean)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (uiState.isLocationPickerVisible) {
        LocationPickerDialog(
            initialLat = uiState.userLatitude ?: 0.0,
            initialLng = uiState.userLongitude ?: 0.0,
            onDismiss = { viewModel.toggleLocationPicker(false) },
            onLocationSelected = { lat, lng, addr -> 
                viewModel.updateLocation(lat, lng, addr)
                Toast.makeText(context, "Location updated successfully", Toast.LENGTH_SHORT).show()
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
    return when {
        parts.size >= 3 -> parts[parts.size - 3].trim()
        parts.size == 2 -> parts[0].trim()
        else -> address.trim()
    }
}

@Composable
fun BottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userAvatarUrl: String? = null
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = true,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Ocean,
                selectedTextColor = Ocean,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Ocean.copy(alpha = 0.1f)
            )
        )
        
        // FAB-like Add Button in the middle
        NavigationBarItem(
            selected = false,
            onClick = onAddClick,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Ocean,
                    modifier = Modifier.size(42.dp),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },
            label = { Text("Add") },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Transparent,
                unselectedTextColor = Color.Gray
            )
        )

        NavigationBarItem(
            icon = {
                if (!userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.size(24.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                }
            },
            label = { Text("Profile") },
            selected = false,
            onClick = onProfileClick,
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

@Composable
fun HomeTopBar(address: String, onLocationClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Color.White, shadowElevation = 1.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onLocationClick() },
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
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { onQueryChange(it.replace("\n", "")) },
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = { Text("Search items...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MutedFgLight) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedBorderColor = Ocean,
                cursorColor = Ocean,
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true,
            maxLines = 1
        )

        // Filter Button
        Box {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ocean.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Tune, "Sort", tint = Ocean)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            onSortChange(option)
                            expanded = false
                        },
                        leadingIcon = {
                            if (selectedSort == option) {
                                Icon(Icons.Default.Check, null, tint = Ocean)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    categories: List<Category>,
    selectedId: String?,
    onCategorySelect: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Categories", "Find what you need")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item {
                CategoryItem(
                    name = "All",
                    icon = Icons.Default.GridView,
                    isSelected = selectedId == null,
                    onClick = { onCategorySelect(null) }
                )
            }
            items(categories) { category ->
                CategoryItem(
                    name = category.name,
                    icon = getCategoryIcon(category.name),
                    isSelected = selectedId == category.id,
                    onClick = { onCategorySelect(category.id) }
                )
            }
        }
    }
}

@Composable
fun CategoryItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) Ocean else Color.White,
            modifier = Modifier.size(60.dp),
            border = if (!isSelected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)) else null,
            shadowElevation = if (isSelected) 4.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    null,
                    tint = if (isSelected) Color.White else Color.DarkGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Ocean else Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
        Text(subtitle, fontSize = 13.sp, color = MutedFgLight)
    }
}

@Composable
fun GlobalRentalItem(item: RentalItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f).height(100.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.categoryId, color = Ocean, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(
                        simplifyAddress(item.location),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(modifier = Modifier.height(100.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
                Text("₹${item.pricePerDay.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Ocean)
                Text("/day", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LocationPickerDialog(
    initialLat: Double,
    initialLng: Double,
    onDismiss: () -> Unit,
    onLocationSelected: (Double, Double, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(initialLat) }
    var longitude by remember { mutableDoubleStateOf(initialLng) }
    
    var suggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var isProgrammaticUpdate by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    fun updateLocationAndAddress(latVal: Double, lngVal: Double, isFromMap: Boolean = false) {
        latitude = latVal
        longitude = lngVal
        if (isFromMap) isProgrammaticUpdate = true
        address = "Fetching address..."
        
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val fetchedAddress = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(latVal, lngVal, 1)?.firstOrNull()?.getAddressLine(0)
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latVal, lngVal, 1)?.firstOrNull()?.getAddressLine(0)
                    }
                }
                address = fetchedAddress ?: "Location at ${String.format("%.4f, %.4f", latVal, lngVal)}"
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    address = "Location at ${String.format("%.4f, %.4f", latVal, lngVal)}"
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                @SuppressLint("MissingPermission")
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        loc?.let { updateLocationAndAddress(it.latitude, it.longitude) }
                    }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Initialize address
    LaunchedEffect(Unit) {
        if (latitude != 0.0 && longitude != 0.0) {
            updateLocationAndAddress(latitude, longitude)
        } else {
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                @SuppressLint("MissingPermission")
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc -> loc?.let { updateLocationAndAddress(it.latitude, it.longitude) } }
            } else {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }

    fun searchAddress(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch(Dispatchers.IO) {
            delay(800)
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("Select Location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(MutedLight).border(1.dp, BorderLight, RoundedCornerShape(20.dp))) {
                    AndroidView<MapView>(
                        factory = { 
                            mapView.apply {
                                val eventsReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        updateLocationAndAddress(p.latitude, p.longitude, isFromMap = true)
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint): Boolean = false
                                }
                                overlays.add(MapEventsOverlay(eventsReceiver))
                            }
                        },
                        update = { view ->
                            if (latitude != 0.0) {
                                val geoPoint = GeoPoint(latitude, longitude)
                                val currentCenter = view.mapCenter
                                val latDiff = Math.abs(currentCenter.latitude - latitude)
                                val lngDiff = Math.abs(currentCenter.longitude - longitude)
                                
                                if (latDiff > 0.0001 || lngDiff > 0.0001) {
                                    view.controller.animateTo(geoPoint)
                                }

                                view.overlays.removeAll { it is Marker }
                                val marker = Marker(view).apply {
                                    position = geoPoint
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "Selected Location"
                                }
                                view.overlays.add(marker)
                            }
                            view.invalidate()
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                    ) {
                        Text("Tap map to select", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it.replace("\n", "")
                                if (!isProgrammaticUpdate && it.length > 3) {
                                    searchAddress(it)
                                } else {
                                    showSuggestions = false
                                }
                                isProgrammaticUpdate = false
                            },
                            placeholder = { Text("Search or tap map", color = MutedFgLight) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Ocean, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fineGranted || coarseGranted) {
                                        @SuppressLint("MissingPermission")
                                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                            .addOnSuccessListener { loc -> loc?.let { updateLocationAndAddress(it.latitude, it.longitude) } }
                                    } else {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                }) {
                                    Icon(Icons.Outlined.MyLocation, "Current location", tint = Ocean, modifier = Modifier.size(20.dp))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), 
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            singleLine = true,
                            maxLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
                        )

                        DropdownMenu(
                            expanded = showSuggestions,
                            onDismissRequest = { showSuggestions = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(Color.White),
                            properties = PopupProperties(focusable = false)
                        ) {
                            suggestions.forEach { addr ->
                                DropdownMenuItem(
                                    text = { Text(addr.getAddressLine(0), color = Color.Black, fontSize = 14.sp) },
                                    onClick = {
                                        isProgrammaticUpdate = true
                                        showSuggestions = false
                                        searchJob?.cancel()
                                        updateLocationAndAddress(addr.latitude, addr.longitude)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Place, null, tint = Ocean, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onLocationSelected(latitude, longitude, address) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ocean),
                    enabled = address.isNotEmpty() && latitude != 0.0 && address != "Fetching address..."
                ) {
                    Text("Confirm Location", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun RazorpaySetupDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Setup Payments") },
        text = {
            Column {
                Text("To receive payments, please enter your Razorpay Account ID.", fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = id,
                    onValueChange = { 
                        if (it.length <= 20) id = it.replace("\n", "") 
                    },
                    label = { Text("Razorpay ID") },
                    placeholder = { Text("acc_...") },
                    singleLine = true,
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Ocean,
                        unfocusedBorderColor = BorderLight,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    )
                )
                Text(
                    text = "Limit: ${id.length}/20",
                    fontSize = 11.sp,
                    color = MutedFgLight,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(id) }, colors = ButtonDefaults.buttonColors(containerColor = Ocean)) {
                Text("Save")
            }
        }
    )
}

fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "electronics" -> Icons.Default.Devices
        "vehicles" -> Icons.Default.DirectionsCar
        "tools" -> Icons.Default.Build
        "sports" -> Icons.Default.SportsBasketball
        "camping" -> Icons.Default.Terrain
        "party" -> Icons.Default.Celebration
        "books" -> Icons.Default.Book
        "appliances" -> Icons.Default.Kitchen
        "camera" -> Icons.Default.CameraAlt
        "musical" -> Icons.Default.MusicNote
        "clothing" -> Icons.Default.Checkroom
        else -> Icons.Default.Category
    }
}

package com.example.borrowbay.features.createlisting.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.borrowbay.features.createlisting.viewmodel.ListingUiState
import com.example.borrowbay.features.createlisting.viewmodel.ProductViewModel
import com.example.borrowbay.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ListingStep {
    PHOTOS, DETAILS, PRICE, LOCATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: ProductViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.listingState
    var currentStep by remember { mutableStateOf(ListingStep.PHOTOS) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageBytesList by remember { mutableStateOf<List<ByteArray>>(emptyList()) }
    var tempPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val fusedLocationClient: FusedLocationProviderClient = remember { 
        LocationServices.getFusedLocationProviderClient(context) 
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f)
    }
    val markerState = rememberMarkerState(position = LatLng(0.0, 0.0))

    LaunchedEffect(uiState) {
        if (uiState is ListingUiState.Success) {
            showSuccessDialog = true
        }
    }

    fun getAddressFromLocation(lat: Double, lng: Double) {
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val fetchedAddress = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
                    }
                }
                if (fetchedAddress != null) {
                    address = fetchedAddress
                    latitude = lat
                    longitude = lng
                    markerState.position = LatLng(lat, lng)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to get address", Toast.LENGTH_SHORT).show()
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
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        location?.let { getAddressFromLocation(it.latitude, it.longitude) }
                    }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    fun createTempPictureUri(): Uri? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            storageDir?.mkdirs()
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "com.example.borrowbay.provider", file)
        } catch (e: Exception) { null }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) {
            val uri = Uri.parse(tempPhotoUriString)
            selectedImages = selectedImages + uri
            try {
                context.contentResolver.openInputStream(uri)?.use { imageBytesList = imageBytesList + it.readBytes() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val newUris = uris.filter { it !in selectedImages }
        selectedImages = selectedImages + newUris
        val newBytes = newUris.mapNotNull { uri ->
            try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
        }
        imageBytesList = imageBytesList + newBytes
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createTempPictureUri()
            if (uri != null) { tempPhotoUriString = uri.toString(); cameraLauncher.launch(uri) }
        }
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (currentStep == ListingStep.PHOTOS) onBack()
        else currentStep = ListingStep.entries[currentStep.ordinal - 1]
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                Button(onClick = {
                    showSuccessDialog = false
                    viewModel.resetListingState()
                    onBack()
                }) { Text("OK", color = OnPrimary) }
            },
            title = { Text("Published!", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = { Text("Your item has been successfully listed on BorrowBay.", color = Color.Black) },
            containerColor = SurfaceLight
        )
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceLight)) {
                CenterAlignedTopAppBar(
                    title = { Text("Create Listing", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black) },
                    navigationIcon = {
                        Surface(
                            onClick = {
                                if (currentStep == ListingStep.PHOTOS) onBack()
                                else currentStep = ListingStep.entries[currentStep.ordinal - 1]
                            },
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
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ListingStep.entries.forEach { step ->
                        Box(
                            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (step.ordinal <= currentStep.ordinal) Ocean else MutedLight)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp, color = SurfaceLight) {
                Box(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = {
                            when (currentStep) {
                                ListingStep.PHOTOS -> currentStep = ListingStep.DETAILS
                                ListingStep.DETAILS -> currentStep = ListingStep.PRICE
                                ListingStep.PRICE -> currentStep = ListingStep.LOCATION
                                ListingStep.LOCATION -> viewModel.listProduct(name, category, desc, rent.toDoubleOrNull() ?: 0.0, security.toDoubleOrNull() ?: 0.0, address, latitude, longitude, imageBytesList)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                        enabled = when (currentStep) {
                            ListingStep.PHOTOS -> selectedImages.isNotEmpty()
                            ListingStep.DETAILS -> name.isNotEmpty() && category.isNotEmpty()
                            ListingStep.PRICE -> rent.isNotEmpty()
                            ListingStep.LOCATION -> address.isNotEmpty()
                        } && uiState !is ListingUiState.Loading
                    ) {
                        if (uiState is ListingUiState.Loading) {
                            CircularProgressIndicator(color = OnPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentStep == ListingStep.LOCATION) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = OnPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Publish Listing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                                } else {
                                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp), tint = OnPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            AnimatedContent(targetState = currentStep, label = "StepTransition") { step ->
                when (step) {
                    ListingStep.PHOTOS -> PhotoStep(selectedImages, { showImageSourceDialog = true }, { idx ->
                        selectedImages = selectedImages.filterIndexed { i, _ -> i != idx }
                        imageBytesList = imageBytesList.filterIndexed { i, _ -> i != idx }
                    })
                    ListingStep.DETAILS -> DetailsStep(name, { name = it }, category, { category = it }, desc, { desc = it })
                    ListingStep.PRICE -> PriceStep(rent, { rent = it }, security, { security = it })
                    ListingStep.LOCATION -> LocationStep(
                        address = address, 
                        cameraPositionState = cameraPositionState,
                        markerState = markerState,
                        onAddressChange = { address = it }, 
                        onMapClick = { latLng -> getAddressFromLocation(latLng.latitude, latLng.longitude) },
                        onUseCurrent = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { loc -> loc?.let { getAddressFromLocation(it.latitude, it.longitude) } }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    )
                }
            }
        }
    }

    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = { showImageSourceDialog = false },
            onGallery = { galleryLauncher.launch("image/*") },
            onCamera = {
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    val uri = createTempPictureUri()
                    if (uri != null) { tempPhotoUriString = uri.toString(); cameraLauncher.launch(uri) }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                showImageSourceDialog = false
            }
        )
    }
}

@Composable
fun PhotoStep(images: List<Uri>, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    Column {
        Text("Add photos of your item", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Good photos help your item rent faster.", color = Color.Gray, fontSize = 16.sp)
        Spacer(Modifier.height(24.dp))
        
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MutedLight)
                    .clickable { onAdd() }
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = Color.Gray, // Add the color here (e.g., Color.Gray or Ocean)
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(48.dp), tint = Ocean)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to add photos", color = Ocean, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(images) { idx, uri ->
                    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { onRemove(idx) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                item {
                    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(MutedLight).clickable { onAdd() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Ocean, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsStep(name: String, onName: (String) -> Unit, category: String, onCategory: (String) -> Unit, desc: String, onDesc: (String) -> Unit) {
    Column {
        Text("What are you listing?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(24.dp))
        
        Text("Item Name", fontWeight = FontWeight.Bold, color = Color.Black)
        OutlinedTextField(value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. Canon EOS R5") }, shape = RoundedCornerShape(12.dp))
        
        Spacer(Modifier.height(16.dp))
        
        Text("Category", fontWeight = FontWeight.Bold, color = Color.Black)
        // Basic category selection
        val categories = listOf("Electronics", "Vehicles", "Sports", "Appliances", "Tools")
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                FilterChip(selected = category == cat, onClick = { onCategory(cat) }, label = { Text(cat) }, shape = RoundedCornerShape(20.dp))
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Description", fontWeight = FontWeight.Bold, color = Color.Black)
        OutlinedTextField(value = desc, onValueChange = onDesc, modifier = Modifier.fillMaxWidth().height(150.dp), placeholder = { Text("Describe the item condition and features...") }, shape = RoundedCornerShape(12.dp))
    }
}

@Composable
fun PriceStep(rent: String, onRent: (String) -> Unit, security: String, onSecurity: (String) -> Unit) {
    Column {
        Text("Set your pricing", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Users pay rent daily.", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        
        Text("Rent per Day (₹)", fontWeight = FontWeight.Bold, color = Color.Black)
        OutlinedTextField(value = rent, onValueChange = onRent, modifier = Modifier.fillMaxWidth(), placeholder = { Text("0.00") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
        
        Spacer(Modifier.height(24.dp))
        
        Text("Security Deposit (₹)", fontWeight = FontWeight.Bold, color = Color.Black)
        OutlinedTextField(value = security, onValueChange = onSecurity, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Optional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
    }
}

@Composable
fun LocationStep(address: String, cameraPositionState: CameraPositionState, markerState: MarkerState, onAddressChange: (String) -> Unit, onMapClick: (LatLng) -> Unit, onUseCurrent: () -> Unit) {
    Column {
        Text("Where is it located?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = address, 
            onValueChange = onAddressChange, 
            modifier = Modifier.fillMaxWidth(), 
            placeholder = { Text("Street address, City, Area") }, 
            trailingIcon = { IconButton(onClick = onUseCurrent) { Icon(Icons.Outlined.MyLocation, null, tint = Ocean) } },
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, MutedLight, RoundedCornerShape(16.dp))) {
            GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState, onMapClick = onMapClick, uiSettings = MapUiSettings(zoomControlsEnabled = false)) {
                if (markerState.position.latitude != 0.0) Marker(state = markerState)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tap on the map to set location exactly", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ImageSourceDialog(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = { Text("Select Image Source", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ListItem(headlineContent = { Text("Gallery") }, leadingContent = { Icon(Icons.Default.PhotoLibrary, null) }, modifier = Modifier.clickable { onGallery(); onDismiss() })
                ListItem(headlineContent = { Text("Camera") }, leadingContent = { Icon(Icons.Default.PhotoCamera, null) }, modifier = Modifier.clickable { onCamera(); onDismiss() })
            }
        },
        containerColor = SurfaceLight
    )
}

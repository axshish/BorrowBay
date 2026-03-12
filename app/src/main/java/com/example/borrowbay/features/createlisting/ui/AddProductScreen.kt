package com.example.borrowbay.features.createlisting.ui

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.borrowbay.features.createlisting.viewmodel.CreateListingViewModel
import com.example.borrowbay.features.createlisting.viewmodel.ListingUiState
import com.example.borrowbay.ui.theme.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ListingStep {
    PHOTOS, DETAILS, PRICE, LOCATION
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddProductScreen(
    viewModel: CreateListingViewModel = viewModel(),
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.listingState
    var currentStep by remember { mutableStateOf(ListingStep.PHOTOS) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Authorization Check
    var isAuthorized by remember { mutableStateOf<Boolean?>(null) }
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                val rzpId = doc.getString("razorpayId")
                isAuthorized = !rzpId.isNullOrBlank()
            } catch (e: Exception) {
                isAuthorized = false
            }
        } else {
            isAuthorized = false
        }
    }

    if (isAuthorized == false) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF718096))
                Spacer(Modifier.height(16.dp))
                Text("Setup Required", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "You need to connect a Razorpay account in your profile before you can list items for rent.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF718096)
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    if (isAuthorized == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Ocean)
        }
        return
    }

    // Initialize OSM Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var name by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
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

    val maxRent = 100000.0
    val maxSecurity = 1000000.0

    LaunchedEffect(uiState) {
        if (uiState is ListingUiState.Success) {
            showSuccessDialog = true
        } else if (uiState is ListingUiState.Error) {
            Toast.makeText(context, (uiState as ListingUiState.Error).msg, Toast.LENGTH_SHORT).show()
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
                @SuppressLint("MissingPermission")
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
            if (storageDir == null) return null
            storageDir.mkdirs()
            val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUriString != null) {
            val uri = Uri.parse(tempPhotoUriString)
            selectedImages = selectedImages + uri
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        val bytes = it.readBytes()
                        withContext(Dispatchers.Main) {
                            imageBytesList = imageBytesList + bytes
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val newUris = uris.filter { it !in selectedImages }
        selectedImages = selectedImages + newUris
        scope.launch(Dispatchers.IO) {
            val newBytes = newUris.mapNotNull { uri ->
                try { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }
            }
            withContext(Dispatchers.Main) {
                imageBytesList = imageBytesList + newBytes
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = createTempPictureUri()
            if (uri != null) {
                tempPhotoUriString = uri.toString()
                cameraLauncher.launch(uri)
            }
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
                    onSuccess()
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
                    val rentVal = rent.toDoubleOrNull() ?: 0.0
                    val securityVal = security.toDoubleOrNull() ?: 0.0
                    val isPriceValid = rentVal > 0 && rentVal <= maxRent && securityVal >= 0 && securityVal <= maxSecurity

                    Button(
                        onClick = {
                            when (currentStep) {
                                ListingStep.PHOTOS -> currentStep = ListingStep.DETAILS
                                ListingStep.DETAILS -> currentStep = ListingStep.PRICE
                                ListingStep.PRICE -> currentStep = ListingStep.LOCATION
                                ListingStep.LOCATION -> viewModel.listProduct(name, selectedCategories.joinToString(", "), desc, rentVal, securityVal, address, latitude, longitude, imageBytesList)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                        enabled = when (currentStep) {
                            ListingStep.PHOTOS -> selectedImages.isNotEmpty()
                            ListingStep.DETAILS -> name.isNotEmpty() && selectedCategories.isNotEmpty() && desc.isNotEmpty()
                            ListingStep.PRICE -> rent.isNotEmpty() && isPriceValid
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
                    ListingStep.DETAILS -> DetailsStep(
                        name, { name = it },
                        selectedCategories, {
                            if (selectedCategories.contains(it)) selectedCategories -= it
                            else selectedCategories += it
                        },
                        desc, { desc = it }
                    )
                    ListingStep.PRICE -> PriceStep(rent, { rent = it }, security, { security = it }, maxRent, maxSecurity)
                    ListingStep.LOCATION -> LocationStep(
                        address = address, 
                        latitude = latitude,
                        longitude = longitude,
                        onAddressChange = { address = it }, 
                        onLocationSelected = { addr, lat, lng ->
                            address = addr
                            latitude = lat
                            longitude = lng
                        },
                        onMapClick = { lat, lng -> getAddressFromLocation(lat, lng) },
                        onUseCurrent = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                @SuppressLint("MissingPermission")
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
                } else { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
        )
    }
}

@Composable
fun PhotoStep(images: List<Uri>, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    Column {
        Text("Add photos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Add up to 8 photos. The first photo will be your cover.", color = MutedFgLight, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(32.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(SurfaceLight)
                        .clickable { onAdd() }
                        .drawDashedBorder(MutedFgLight, 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, tint = MutedFgLight, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Add Photo", color = MutedFgLight, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            itemsIndexed(images) { index, uri ->
                Box(modifier = Modifier.aspectRatio(1f)) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                    IconButton(onClick = { onRemove(index) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            items(maxOf(0, 5 - images.size)) { Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MutedLight)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsStep(name: String, onNameChange: (String) -> Unit, selectedCategories: Set<String>, onCatToggle: (String) -> Unit, desc: String, onDescChange: (String) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Item details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Title", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            if (name.isEmpty()) Text(" *", color = Color.Red, fontSize = 15.sp)
        }
        OutlinedTextField(
            value = name, onValueChange = onNameChange, placeholder = { Text("Enter item title", color = MutedFgLight) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
        )

        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pick Relevant Categories", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            if (selectedCategories.isEmpty()) Text(" *", color = Color.Red, fontSize = 15.sp)
        }
        Text("Select all that apply to your item", color = MutedFgLight, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val categories = listOf("Electronics" to Icons.Default.CameraAlt, "Sports" to Icons.AutoMirrored.Filled.DirectionsBike, "Tools" to Icons.Default.Build, "Outdoors" to Icons.Default.Terrain, "Vehicles" to Icons.Default.DirectionsCar, "Music" to Icons.Default.MusicNote, "Gaming" to Icons.Default.Gamepad)
            categories.forEach { (cat, icon) ->
                val selected = selectedCategories.contains(cat)
                Surface(
                    onClick = { onCatToggle(cat) },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, if (selected) Ocean else BorderLight),
                    color = if (selected) Ocean.copy(alpha = 0.1f) else SurfaceLight
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) Ocean else MutedFgLight)
                        Spacer(Modifier.width(8.dp))
                        Text(cat, color = if (selected) Ocean else Color.Black, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Description", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
            if (desc.isEmpty()) Text(" *", color = Color.Red, fontSize = 15.sp)
        }
        OutlinedTextField(
            value = desc, onValueChange = onDescChange, placeholder = { Text("Enter item description", color = MutedFgLight) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(120.dp), shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
        )
    }
}

@Composable
fun PriceStep(rent: String, onRentChange: (String) -> Unit, security: String, onSecurityChange: (String) -> Unit, maxRent: Double, maxSecurity: Double) {
    Column {
        Text("Set your price", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(32.dp))
        
        Text("Price per day (₹)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
        OutlinedTextField(
            value = rent,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onRentChange(it) },
            placeholder = { Text("Enter amount", color = MutedFgLight) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
            isError = (rent.toDoubleOrNull() ?: 0.0) > maxRent
        )
        if ((rent.toDoubleOrNull() ?: 0.0) > maxRent) {
            Text("Maximum rent allowed is ₹1,00,000", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text("Security deposit (₹)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
        OutlinedTextField(
            value = security,
            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onSecurityChange(it) },
            placeholder = { Text("Enter amount", color = MutedFgLight) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black),
            isError = (security.toDoubleOrNull() ?: 0.0) > maxSecurity
        )
        if ((security.toDoubleOrNull() ?: 0.0) > maxSecurity) {
            Text("Maximum security allowed is ₹10,00,000", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

@Composable
fun LocationStep(
    address: String,
    latitude: Double,
    longitude: Double,
    onAddressChange: (String) -> Unit,
    onLocationSelected: (String, Double, Double) -> Unit,
    onMapClick: (Double, Double) -> Unit,
    onUseCurrent: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun searchAddress(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch(Dispatchers.IO) {
            delay(500) // Debounce
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

    Column {
        Text("Set pickup location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.Black)
        Text("Choose where renters can pick up the item.", color = MutedFgLight, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(32.dp))

        Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(20.dp)).background(MutedLight).border(1.dp, BorderLight, RoundedCornerShape(20.dp))) {
            AndroidView<MapView>(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        val eventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                onMapClick(p.latitude, p.longitude)
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
                        val dist = (currentCenter.latitude - latitude) * (currentCenter.latitude - latitude) +
                                   (currentCenter.longitude - longitude) * (currentCenter.longitude - longitude)
                        if (dist > 0.00001) {
                            view.controller.animateTo(geoPoint)
                        }

                        view.overlays.removeAll { it is Marker }
                        val marker = Marker(view).apply {
                            position = geoPoint
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Pickup Location"
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
                Text("Tap on map to select", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Box {
            OutlinedTextField(
                value = address,
                onValueChange = {
                    onAddressChange(it)
                    if (it.length > 3) searchAddress(it) else {
                        showSuggestions = false
                        searchJob?.cancel()
                    }
                },
                placeholder = { Text("Enter address manually or use map", color = MutedFgLight) },
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Ocean, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    IconButton(onClick = onUseCurrent) {
                        Icon(Icons.Outlined.MyLocation, "Use current location", tint = Ocean, modifier = Modifier.size(20.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ocean, unfocusedBorderColor = BorderLight, unfocusedContainerColor = SurfaceLight, focusedContainerColor = SurfaceLight, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)
            )

            if (showSuggestions) {
                DropdownMenu(
                    expanded = showSuggestions,
                    onDismissRequest = { showSuggestions = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(SurfaceLight),
                    properties = PopupProperties(focusable = false)
                ) {
                    suggestions.forEach { addr ->
                        DropdownMenuItem(
                            text = { Text(addr.getAddressLine(0), color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showSuggestions = false
                                searchJob?.cancel()
                                onLocationSelected(addr.getAddressLine(0), addr.latitude, addr.longitude)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSourceDialog(onDismiss: () -> Unit, onGallery: () -> Unit, onCamera: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceLight,
        title = { Text("Add Photo", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = { Text("Choose a source for your product photo", color = Color.Black) },
        confirmButton = { TextButton(onClick = { onDismiss(); onGallery() }) { Text("Gallery", color = Ocean) } },
        dismissButton = { TextButton(onClick = { onDismiss(); onCamera() }) { Text("Camera", color = Ocean) } }
    )
}

fun Modifier.drawDashedBorder(color: Color, cornerRadius: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.drawWithContent {
        drawContent()
        val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        drawRoundRect(color = color, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx()))
    }
)

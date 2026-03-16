package com.example.borrowbay.features.productdetail.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.features.productdetail.viewmodel.ProductDetailViewModel
import com.example.borrowbay.features.productdetail.viewmodel.ProductDetailViewModelFactory
import com.example.borrowbay.ui.theme.*
import com.example.borrowbay.util.RazorpayHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: ProductDetailViewModel = viewModel(factory = ProductDetailViewModelFactory(productId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    
    val razorpayHelper = remember {
        activity?.let {
            RazorpayHelper(it, onSuccess = {
                viewModel.rentProduct {
                    onPaymentSuccess()
                }
            }, onError = { code, desc ->
                Toast.makeText(context, "Payment Failed: $desc", Toast.LENGTH_LONG).show()
            })
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            if (uiState.item != null && uiState.item!!.isAvailable) {
                RentNowBottomBar(
                    isLoading = uiState.isRenting,
                    onRentClick = {
                        val auth = FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        if (currentUser != null && razorpayHelper != null) {
                            val totalInPaisa = (uiState.totalPrice * 100).toInt()
                            val depositInPaisa = (uiState.securityDeposit * 100).toInt()
                            
                            razorpayHelper.startPayment(
                                totalAmountInPaisa = totalInPaisa,
                                itemName = uiState.item!!.name,
                                userEmail = currentUser.email ?: "",
                                userContact = "9999999999", 
                                merchantId = uiState.item!!.owner?.razorpayId ?: uiState.item!!.ownerId, 
                                depositAmountInPaisa = depositInPaisa
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Ocean)
            } else if (uiState.error != null) {
                Text(uiState.error!!, modifier = Modifier.align(Alignment.Center), color = Destructive)
            } else uiState.item?.let { item ->
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ImageCarousel(item.imageUrls, onBackClick)
                    
                    Column(modifier = Modifier.padding(24.dp)) {
                        ProductHeader(item, uiState.calculatedDistance)
                        Spacer(modifier = Modifier.height(24.dp))
                        SellerCard(item)
                        Spacer(modifier = Modifier.height(32.dp))
                        DescriptionSection(item.description ?: "")
                        Spacer(modifier = Modifier.height(32.dp))
                        PriceBreakdownCard(uiState)
                        Spacer(modifier = Modifier.height(24.dp))
                        RentalDaysPicker(
                            days = uiState.rentalDays,
                            onDaysChange = { viewModel.updateRentalDays(it) }
                        )
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ImageCarousel(imageUrls: List<String>, onBackClick: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    
    LaunchedEffect(Unit) {
        while (imageUrls.size > 1) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % imageUrls.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(380.dp).background(MutedLight)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Surface(
            onClick = onBackClick,
            shape = CircleShape,
            color = SurfaceLight,
            shadowElevation = 2.dp,
            modifier = Modifier.statusBarsPadding().padding(16.dp).size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp), tint = Color.Black)
            }
        }

        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(imageUrls.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .size(width = if (isSelected) 20.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Ocean else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun ProductHeader(item: RentalItem, distance: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Ocean, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${String.format("%.1f", distance)} km away", color = MutedFgLight, fontSize = 14.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${item.pricePerDay.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Ocean)
            Text("per day", fontSize = 12.sp, color = MutedFgLight)
        }
    }
}

@Composable
fun SellerCard(item: RentalItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!item.owner?.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.owner?.avatarUrl,
                    contentDescription = "Seller Avatar",
                    modifier = Modifier.size(52.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(52.dp).background(MutedLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (item.owner?.name ?: "??").take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Ocean
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.owner?.name ?: "Unknown Seller", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text("Verified Lender", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.owner?.phone}"))
                    context.startActivity(intent)
                }, modifier = Modifier.background(MutedLight, CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Phone, null, tint = Ocean, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${item.owner?.email}"))
                    context.startActivity(intent)
                }, modifier = Modifier.background(MutedLight, CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Email, null, tint = Ocean, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DescriptionSection(description: String) {
    Column {
        Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Text(description, color = SlateDeepLight, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

@Composable
fun PriceBreakdownCard(state: com.example.borrowbay.features.productdetail.viewmodel.ProductDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Price Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            PriceRow("Rental (${state.rentalDays} day)", "₹${state.subtotalPrice.toInt()}")
            PriceRow("Security deposit", "₹${state.securityDeposit.toInt()}", isHighlight = true)
            PriceRow("Platform fee", "₹${state.platformFee.toInt()}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = BorderLight)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Text("₹${state.totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ocean)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = Emerald.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = Emerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Deposit refunded upon safe return", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: String, isHighlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MutedFgLight, fontSize = 14.sp)
        Text(amount, color = if (isHighlight) AmberDark else Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun RentalDaysPicker(days: Int, onDaysChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rental Duration", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDaysChange(days - 1) }, enabled = days > 1) {
                    Icon(Icons.Default.RemoveCircle, null, tint = if (days > 1) Ocean else MutedLight, modifier = Modifier.size(28.dp))
                }
                Text("$days Days", modifier = Modifier.width(70.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                IconButton(onClick = { onDaysChange(days + 1) }) {
                    Icon(Icons.Default.AddCircle, null, tint = Ocean, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun RentNowBottomBar(isLoading: Boolean, onRentClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = SurfaceLight
    ) {
        Box(
            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onRentClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ocean, contentColor = OnPrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = OnPrimary)
                } else {
                    // Removed per day rent from the button
                    Text("Rent Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

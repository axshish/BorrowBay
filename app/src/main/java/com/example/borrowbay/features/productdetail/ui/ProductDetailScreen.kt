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
        bottomBar = {
            if (uiState.item != null && uiState.item!!.isAvailable) {
                RentNowBottomBar(
                    pricePerDay = uiState.item!!.pricePerDay,
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
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Ocean)
            } else if (uiState.error != null) {
                Text(uiState.error!!, modifier = Modifier.align(Alignment.Center), color = Color.Red)
            } else uiState.item?.let { item ->
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ImageCarousel(item.imageUrls, onBackClick)
                    
                    Column(modifier = Modifier.padding(20.dp)) {
                        ProductHeader(item, uiState.calculatedDistance)
                        Spacer(modifier = Modifier.height(24.dp))
                        SellerCard(item)
                        Spacer(modifier = Modifier.height(24.dp))
                        DescriptionSection(item.description)
                        Spacer(modifier = Modifier.height(24.dp))
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

    Box(modifier = Modifier.fillMaxWidth().height(350.dp).background(Color(0xFFF5F5F5))) {
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

        IconButton(
            onClick = onBackClick,
            modifier = Modifier.statusBarsPadding().padding(16.dp).background(Color.White, CircleShape).size(40.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp))
        }

        if (imageUrls.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(imageUrls.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Ocean else Color.White.copy(alpha = 0.5f)
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
fun ProductHeader(item: RentalItem, distance: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${String.format("%.1f", distance)} km away", color = Color.Gray, fontSize = 14.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹${item.pricePerDay.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ocean)
            Text("per day", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SellerCard(item: RentalItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!item.owner?.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.owner?.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(50.dp).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text((item.owner?.name ?: "??").take(2).uppercase(), fontWeight = FontWeight.Bold, color = Ocean)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.owner?.name ?: "Unknown Seller", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Lender", color = Ocean, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.owner?.phone}"))
                    context.startActivity(intent)
                }, modifier = Modifier.background(Color.White, CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Phone, null, tint = Ocean, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${item.owner?.email}"))
                    context.startActivity(intent)
                }, modifier = Modifier.background(Color.White, CircleShape).size(40.dp)) {
                    Icon(Icons.Default.Email, null, tint = Ocean, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun DescriptionSection(description: String) {
    Column {
        Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, color = Color.DarkGray, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun PriceBreakdownCard(state: com.example.borrowbay.features.productdetail.viewmodel.ProductDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Price Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            PriceRow("Rental (${state.rentalDays} day)", "₹${state.subtotalPrice.toInt()}")
            PriceRow("Security deposit", "₹${state.securityDeposit.toInt()}", isHighlight = true)
            PriceRow("Platform fee", "₹${state.platformFee.toInt()}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("₹${state.totalPrice.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = Emerald, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Deposit refunded upon safe return", color = Emerald, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PriceRow(label: String, amount: String, isHighlight: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(amount, color = if (isHighlight) Color(0xFFDAA520) else Color.Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun RentalDaysPicker(days: Int, onDaysChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rental Duration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDaysChange(days - 1) }, enabled = days > 1) {
                    Icon(Icons.Default.RemoveCircleOutline, null, tint = if (days > 1) Ocean else Color.Gray)
                }
                Text("$days Days", modifier = Modifier.width(60.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onDaysChange(days + 1) }) {
                    Icon(Icons.Default.AddCircleOutline, null, tint = Ocean)
                }
            }
        }
    }
}

@Composable
fun RentNowBottomBar(pricePerDay: Double, isLoading: Boolean, onRentClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Box(
            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onRentClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ocean),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Rent Now — ₹${pricePerDay.toInt()}/day", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

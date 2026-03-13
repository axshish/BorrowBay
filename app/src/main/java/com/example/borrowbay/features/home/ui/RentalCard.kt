package com.example.borrowbay.features.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.borrowbay.data.model.RentalItem
import java.util.concurrent.TimeUnit

@Composable
fun RentalCard(
    item: RentalItem,
    modifier: Modifier = Modifier,
    showRenterInfo: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val mainImage = item.imageUrls.firstOrNull()
                
                if (mainImage != null) {
                    AsyncImage(
                        model = mainImage,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(48.dp),
                            tint = Color.Gray
                        )
                    }
                }

                // Status Badge
                val isActuallyAvailable = item.isAvailable && item.renterId == null
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart),
                    color = if (isActuallyAvailable) Color(0xFF48BB78) else Color(0xFFF56565),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isActuallyAvailable) "Available" else "On Rent",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Duration Tracking Badge
                if (!isActuallyAvailable && item.rentedAt != null) {
                    val durationMillis = System.currentTimeMillis() - item.rentedAt
                    val daysElapsed = TimeUnit.MILLISECONDS.toDays(durationMillis)
                    val totalDays = item.rentalDurationDays ?: 0
                    
                    val durationText = when {
                        daysElapsed >= totalDays -> "Overdue by ${daysElapsed - totalDays} days"
                        daysElapsed == 0L -> "Rented Today (Day 1/$totalDays)"
                        else -> "Day ${daysElapsed + 1} of $totalDays"
                    }

                    Surface(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.BottomEnd),
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = durationText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "₹${item.pricePerDay.toInt()}/day",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF3182CE),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location.split(",").firstOrNull() ?: item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (if (showRenterInfo) "R" else item.owner?.name?.take(1) ?: "?").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4A5568),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showRenterInfo) "Rented by User" else (item.owner?.name ?: "Unknown"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A5568)
                    )
                }
            }
        }
    }
}

package com.example.borrowbay.features.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.borrowbay.data.model.RentalItem
import com.example.borrowbay.ui.theme.Emerald
import com.example.borrowbay.ui.theme.Ocean

@Composable
fun RentalCard(
    item: RentalItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.padding(bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Added slight elevation for better visibility
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(8.dp)) { // Added inner padding to the whole card
            // Image Section with rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                AsyncImage(
                    model = item.imageUrls.firstOrNull(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Availability Badge
                Surface(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.isAvailable) Emerald.copy(alpha = 0.9f) else Color.Gray.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = if (item.isAvailable) "Available" else "Rented",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${item.pricePerDay.toInt()}",
                            fontSize = 16.sp,
                            color = Ocean,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/day",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                    
                    Text(
                        text = "⊙ ${String.format("%.1f", item.distance)} km",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Owner Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.owner?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.owner?.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFEEEEEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (item.owner?.name ?: "??").take(2).uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.owner?.name ?: "Unknown",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

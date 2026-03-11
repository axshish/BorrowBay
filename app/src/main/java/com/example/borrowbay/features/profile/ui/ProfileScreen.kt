package com.example.borrowbay.features.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.borrowbay.R
import com.example.borrowbay.features.profile.model.UserProfile

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    onProfileClick: () -> Unit,
    onActiveListingsClick: () -> Unit,
    onRentalHistoryClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5)) // Modern soft grey background
            .padding(20.dp)
    ) {
        Text(
            text = "Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1C1E21),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Profile Details Card with a subtle gradient background
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() },
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.White, Color(0xFFF0F7FF))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE4E6EB))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.image),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF1C1E21)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = profile.phone, color = Color(0xFF65676B), fontSize = 14.sp)
                        Text(text = profile.email, color = Color(0xFF65676B), fontSize = 14.sp)
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB0B3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Account Settings",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF65676B),
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                ProfileMenuItem(
                    title = "Active Listings",
                    icon = painterResource(id = R.drawable.activity),
                    onClick = onActiveListingsClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp), 
                    thickness = 0.5.dp, 
                    color = Color(0xFFF0F2F5)
                )
                ProfileMenuItem(
                    title = "Rental History",
                    icon = painterResource(id = R.drawable.history),
                    onClick = onRentalHistoryClick
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { /* Sign Out */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Sign Out",
                color = Color(0xFFD32F2F),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ProfileMenuItem(title: String, icon: Painter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF0F2F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1C1E21)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFB0B3B8)
        )
    }
}

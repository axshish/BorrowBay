package com.example.borrowbay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ── Shapes ──
val BorrowBayShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // radius - 8
    small      = RoundedCornerShape(8.dp),   // radius - 4
    medium     = RoundedCornerShape(10.dp),  // radius - 2
    large      = RoundedCornerShape(12.dp),  // radius
    extraLarge = RoundedCornerShape(16.dp),  // radius + 4
)

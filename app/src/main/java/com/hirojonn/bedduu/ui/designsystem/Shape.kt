package com.hirojonn.bedduu.ui.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object BedduuShapes {
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(20.dp)
    val Large = RoundedCornerShape(28.dp)
    val Pill = CircleShape
    val Cover = RoundedCornerShape(12.dp)
    val Material = Shapes(small = Small, medium = Medium, large = Large)
}

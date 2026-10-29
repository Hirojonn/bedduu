package com.hirojonn.bedduu.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BedduuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BedduuColors.Scheme,
        typography = bedduuTypography(),
        shapes = BedduuShapes.Material,
        content = content
    )
}

package com.hirojonn.bedduu.ui.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object BedduuColors {
    val Background = Color(0xFF121117)
    val Surface = Color(0xFF1C1B22)
    val SurfaceVariant = Color(0xFF23222B)
    val Outline = Color(0xFF2A2933)
    val Primary = Color(0xFF7C4DFF)
    val OnPrimary = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8E8CA0)
    val TextDisabled = Color(0xFF5A5868)
    val Error = Color(0xFFFF5C6C)
    val Success = Color(0xFF3DDC97)

    val Scheme = darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = Color(0xFF35246A),
        onPrimaryContainer = TextPrimary,
        secondary = Primary,
        onSecondary = OnPrimary,
        secondaryContainer = SurfaceVariant,
        onSecondaryContainer = TextPrimary,
        tertiary = Success,
        onTertiary = Background,
        tertiaryContainer = Color(0xFF153C30),
        onTertiaryContainer = TextPrimary,
        background = Background,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = TextSecondary,
        surfaceTint = Primary,
        inverseSurface = TextPrimary,
        inverseOnSurface = Background,
        inversePrimary = Color(0xFF5A32D6),
        error = Error,
        onError = OnPrimary,
        errorContainer = Color(0xFF5C1F2B),
        onErrorContainer = TextPrimary,
        outline = Outline,
        outlineVariant = Outline,
        scrim = Color.Black
    )
}

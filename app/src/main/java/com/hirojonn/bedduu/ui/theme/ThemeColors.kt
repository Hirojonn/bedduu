package com.hirojonn.bedduu.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkMode = staticCompositionLocalOf { true }
val LocalPureBlack  = staticCompositionLocalOf { true }

// ─── Dynamic Palette (Adapts reactively to Dark/Light mode) ─────────────────
val BackgroundColor: Color
    @Composable
    get() = if (LocalIsDarkMode.current) {
        if (LocalPureBlack.current) Color(0xFF000000) else Color(0xFF08141C)
    } else {
        Color(0xFFF2F8FB)
    }

val SurfaceColor: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFF10232E) else Color(0xFFFFFFFF)

val SurfaceVariant: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFF1B3442) else Color(0xFFDDECF3)

// Accent — ocean blue
val PrimaryColor     = Color(0xFF087EA4)
val PrimaryDark      = Color(0xFF075776)
val PrimaryLight     = Color(0xFF42B7D8)

// Text
val TextMain: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFFEAF5FA) else Color(0xFF10212B)

val TextMuted: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFF9BB2BE) else Color(0xFF536B78)

val TextSubtle: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFF68818E) else Color(0xFF8297A2)

// Status / Utility
val GreenAccent      = Color(0xFF4ADE80)
val RedAccent        = Color(0xFFFF6B6B)
val OrangeAccent     = Color(0xFFFFAA5C)

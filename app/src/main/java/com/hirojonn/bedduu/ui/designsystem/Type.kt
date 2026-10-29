package com.hirojonn.bedduu.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.text.googlefonts.Font as GoogleFontResource
import androidx.compose.ui.unit.sp
import com.hirojonn.bedduu.R

@Composable
fun bedduuTypography(): Typography {
    val provider = Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )
    val jakarta = GoogleFont("Plus Jakarta Sans")
    val family = FontFamily(
        GoogleFontResource(jakarta, provider, FontWeight.Normal),
        GoogleFontResource(jakarta, provider, FontWeight.Medium)
    )

    return Typography(
        displayLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 39.2.sp
        ),
        headlineSmall = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleMedium = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.4.sp
        ),
        bodyLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.4.sp
        ),
        labelLarge = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.8.sp
        ),
        bodySmall = androidx.compose.ui.text.TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.8.sp
        )
    )
}

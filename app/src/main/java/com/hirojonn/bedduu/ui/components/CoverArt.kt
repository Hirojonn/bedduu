package com.hirojonn.bedduu.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import com.hirojonn.bedduu.ui.theme.SurfaceVariant
import com.hirojonn.bedduu.ui.theme.TextSubtle

@Composable
fun CoverArt(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    imageLoader: ImageLoader? = null
) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url.takeIf(String::isNotBlank))
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        imageLoader = imageLoader ?: context.imageLoader,
        modifier = modifier.background(SurfaceVariant),
        loading = {
            val transition = rememberInfiniteTransition(label = "cover-shimmer")
            val offset by transition.animateFloat(
                initialValue = -1f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100),
                    repeatMode = RepeatMode.Restart
                ),
                label = "cover-shimmer-offset"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                SurfaceVariant,
                                SurfaceVariant.copy(alpha = 0.55f),
                                SurfaceVariant
                            ),
                            start = androidx.compose.ui.geometry.Offset(offset * 500f, 0f),
                            end = androidx.compose.ui.geometry.Offset((offset + 1f) * 500f, 500f)
                        )
                    )
            )
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = "Cover unavailable",
                    tint = TextSubtle,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        success = { SubcomposeAsyncImageContent() }
    )
}

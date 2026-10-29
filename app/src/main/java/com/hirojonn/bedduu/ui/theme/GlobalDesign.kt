package com.hirojonn.bedduu.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.hirojonn.bedduu.R

// ═════════════════════════════════════════════════════════════════════════════
// GLOBAL DESIGN SYSTEM TOKENS
// ═════════════════════════════════════════════════════════════════════════════

object BedduuTokens {
    // Shapes
    val ShapeSmall = RoundedCornerShape(14.dp)
    val ShapeMedium = RoundedCornerShape(20.dp)
    val ShapeLarge = RoundedCornerShape(28.dp)
    val ShapePill = CircleShape

    // Gradients
    val PrimaryGradient = Brush.horizontalGradient(listOf(PrimaryLight, PrimaryColor))
    val DarkSurfaceGradient = Brush.verticalGradient(
        listOf(Color(0xFF173444), Color(0xFF10232E))
    )

    val ScreenBackground: Brush
        @Composable
        get() = Brush.verticalGradient(
            listOf(
                BackgroundColor,
                androidx.compose.ui.graphics.lerp(
                    BackgroundColor,
                    PrimaryColor,
                    if (LocalIsDarkMode.current) 0.12f else 0.045f
                ),
                BackgroundColor
            )
        )

    val GlassSurface: Color
        @Composable
        get() = if (LocalIsDarkMode.current) {
            Color(0xFF183443).copy(alpha = 0.78f)
        } else {
            Color.White.copy(alpha = 0.74f)
        }

    // Border strokes
    val BorderSubtle: Color
        @Composable
        get() = if (LocalIsDarkMode.current) Color(0xFF23404D) else Color(0xFFD4E4EA)

    val GlassBorder: Color
        @Composable
        get() = if (LocalIsDarkMode.current) {
            Color.White.copy(alpha = 0.16f)
        } else {
            Color(0xFF4C9DBA).copy(alpha = 0.24f)
        }
}

// ═════════════════════════════════════════════════════════════════════════════
// GLOBAL REUSABLE UI COMPONENTS
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Standard Bedduu Card container with consistent styling, rounded corners and subtle border.
 */
@Composable
fun BedduuCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = BedduuTokens.ShapeMedium,
    backgroundColor: Color = BedduuTokens.GlassSurface,
    borderColor: Color = BedduuTokens.GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.bedduuSpringClickable(onClick)
    } else Modifier

    Surface(
        shape = shape,
        color = backgroundColor,
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = PrimaryColor.copy(alpha = 0.08f),
                spotColor = PrimaryColor.copy(alpha = 0.12f)
            )
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .then(clickModifier)
    ) {
        Column(content = content)
    }
}

@Composable
fun Modifier.bedduuSpringClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = rememberSpringPressedScale(isPressed)

    return this
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

@Composable
private fun rememberSpringPressedScale(isPressed: Boolean): Float {
    var scale by remember { mutableFloatStateOf(1f) }
    val animation = remember {
        SpringAnimation(FloatValueHolder(1f)).apply {
            spring = SpringForce(1f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            addUpdateListener { _, value, _ -> scale = value }
        }
    }

    LaunchedEffect(isPressed, animation) {
        animation.animateToFinalPosition(if (isPressed) 0.975f else 1f)
    }
    DisposableEffect(animation) {
        onDispose { animation.cancel() }
    }

    return scale
}

/**
 * Global Screen Header with unified styling, title, optional back button, and optional actions.
 */
@Composable
fun BedduuHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceColor)
                    .border(1.dp, BedduuTokens.BorderSubtle, CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextMain,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = trailingActions
        )
    }
}

/**
 * Global Section Title with optional count badge or action text.
 */
@Composable
fun BedduuSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
            if (count != null) {
                Spacer(Modifier.width(8.dp))
                BedduuBadge(text = count.toString())
            }
        }

        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryLight,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Global Empty State (NO EMOJIS — Uses clean modern vector icons).
 */
@Composable
fun BedduuEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(SurfaceColor)
                .border(1.dp, BedduuTokens.BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = BedduuTokens.ShapeSmall
            ) {
                Text(actionText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Global Pill Badge for counts, sources, or tags.
 */
@Composable
fun BedduuBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimaryColor.copy(alpha = 0.18f),
    textColor: Color = PrimaryLight
) {
    Surface(
        shape = BedduuTokens.ShapePill,
        color = backgroundColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Global Stat Box Card (used in Profile and analytics).
 */
@Composable
fun BedduuStatBox(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = TextMain
) {
    BedduuCard(
        modifier = modifier,
        shape = BedduuTokens.ShapeMedium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PrimaryLight,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = value,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    }
}

/**
 * Global Avatar component displaying either the elf-peek logo or vector anime character profile.
 */
@Composable
fun BedduuAvatar(
    avatarIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showBorder: Boolean = true
) {
    val avatarInfo = GLOBAL_AVATARS.getOrElse(avatarIndex) { GLOBAL_AVATARS[0] }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (showBorder) Modifier.border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                else Modifier
            )
            .background(Brush.linearGradient(avatarInfo.gradient)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarInfo.isElfPeekLogo) {
            // Render the official elf-peek icon
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.elf_peek),
                contentDescription = avatarInfo.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = avatarInfo.icon,
                contentDescription = avatarInfo.name,
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

/**
 * Global Character Avatar Model (pure vector/drawables, zero emojis).
 */
data class GlobalAvatarItem(
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val isElfPeekLogo: Boolean = false
)

val GLOBAL_AVATARS = listOf(
    GlobalAvatarItem(
        name = "Elf Peek (Official)",
        subtitle = "Bendduu Mascot",
        icon = Icons.Default.Face,
        gradient = listOf(Color(0xFF1E2132), Color(0xFF141622)),
        isElfPeekLogo = true
    ),
    GlobalAvatarItem(
        name = "Swordsman",
        subtitle = "Action & Combat",
        icon = Icons.Default.Shield,
        gradient = listOf(Color(0xFF00B894), Color(0xFF00CEC9))
    ),
    GlobalAvatarItem(
        name = "Shadow Monarch",
        subtitle = "Dark Fantasy",
        icon = Icons.Default.Visibility,
        gradient = listOf(Color(0xFF2D3436), Color(0xFF636E72))
    ),
    GlobalAvatarItem(
        name = "Sorcerer Supreme",
        subtitle = "Magic & Spells",
        icon = Icons.Default.AutoAwesome,
        gradient = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
    ),
    GlobalAvatarItem(
        name = "Grand Champion",
        subtitle = "Shonen Master",
        icon = Icons.Default.WorkspacePremium,
        gradient = listOf(Color(0xFFE17055), Color(0xFFFAB1A0))
    ),
    GlobalAvatarItem(
        name = "Star Reader",
        subtitle = "Slice of Life",
        icon = Icons.Default.Stars,
        gradient = listOf(Color(0xFF0984E3), Color(0xFF74B9FF))
    )
)

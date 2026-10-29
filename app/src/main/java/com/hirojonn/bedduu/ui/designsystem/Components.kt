package com.hirojonn.bedduu.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun BedduuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = BedduuShapes.Medium,
        color = BedduuColors.Surface,
        contentColor = BedduuColors.TextPrimary,
        border = BorderStroke(1.dp, BedduuColors.Outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(BedduuDimens.Space16), content = content)
    }
}

@Composable
fun CoverCard(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    badge: (@Composable BoxScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier.then(clickModifier)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(BedduuShapes.Cover)
                .background(BedduuColors.SurfaceVariant)
        ) {
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            badge?.invoke(this)
        }
        Text(
            title,
            modifier = Modifier.padding(top = BedduuDimens.Space8),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BedduuPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = BedduuButtonSurface(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    fill = BedduuColors.Primary,
    foreground = BedduuColors.OnPrimary,
    border = null
)

@Composable
fun BedduuSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = BedduuButtonSurface(
    text = text,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    fill = Color.Transparent,
    foreground = BedduuColors.TextPrimary,
    border = BedduuColors.Outline
)

@Composable
fun BedduuTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minHeight = 48.dp),
    shape = BedduuShapes.Pill,
    colors = ButtonDefaults.textButtonColors(contentColor = BedduuColors.Primary)
) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun BedduuButtonSurface(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    fill: Color,
    foreground: Color,
    border: Color?
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = MotionSpec.pressSpring(),
        label = "button-press"
    )
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(BedduuShapes.Pill)
            .background(if (enabled) fill else BedduuColors.TextDisabled.copy(alpha = 0.25f))
            .then(
                if (border != null) Modifier.border(
                    BorderStroke(1.dp, border),
                    BedduuShapes.Pill
                ) else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactions,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = BedduuDimens.Space24, vertical = BedduuDimens.Space12),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = foreground, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BedduuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        animationSpec = MotionSpec.pressSpring(),
        label = "icon-button-press"
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(BedduuShapes.Pill)
            .background(BedduuColors.SurfaceVariant)
            .clickable(
                interactionSource = interactions,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(BedduuDimens.Space12)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun BedduuChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = BedduuShapes.Pill,
        color = if (selected) BedduuColors.Primary.copy(alpha = 0.18f) else BedduuColors.SurfaceVariant,
        contentColor = if (selected) BedduuColors.Primary else BedduuColors.TextSecondary,
        border = BorderStroke(
            1.dp,
            if (selected) BedduuColors.Primary else BedduuColors.Outline
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = BedduuDimens.Space16, vertical = BedduuDimens.Space8),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun BedduuBadge(
    text: String,
    modifier: Modifier = Modifier,
    isNew: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = BedduuShapes.Pill,
        color = if (isNew) BedduuColors.Success else BedduuColors.Primary,
        contentColor = if (isNew) BedduuColors.Background else BedduuColors.OnPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            if (!isNew && text.toIntOrNull() != null && text.toInt() > 99) "99+" else text,
            modifier = Modifier.padding(horizontal = BedduuDimens.Space8, vertical = BedduuDimens.Space4),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun BedduuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = BedduuColors.TextPrimary),
        decorationBox = { inner ->
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = BedduuShapes.Pill,
                color = BedduuColors.SurfaceVariant,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(Modifier.padding(horizontal = BedduuDimens.Space16, vertical = BedduuDimens.Space12)) {
                    if (value.isEmpty()) Text(placeholder, color = BedduuColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
                    inner()
                }
            }
        }
    )
}

@Composable
fun BedduuSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search manga"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BedduuShapes.Pill)
            .background(BedduuColors.SurfaceVariant)
            .padding(horizontal = BedduuDimens.Space16, vertical = BedduuDimens.Space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = BedduuColors.TextSecondary)
        Spacer(Modifier.width(BedduuDimens.Space8))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = BedduuColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = BedduuColors.TextPrimary)
            )
        }
    }
}

@Composable
fun BedduuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = BedduuColors.OnPrimary,
            checkedTrackColor = BedduuColors.Primary,
            checkedBorderColor = BedduuColors.Primary
        )
    )
}

@Composable
fun BedduuSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = BedduuColors.Primary,
            activeTrackColor = BedduuColors.Primary,
            inactiveTrackColor = BedduuColors.Outline
        )
    )
}

@Composable
fun BedduuProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = progress.coerceIn(0f, 1f),
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape),
        color = BedduuColors.Primary,
        trackColor = BedduuColors.SurfaceVariant
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BedduuBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp
        ),
        containerColor = BedduuColors.Surface,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = BedduuDimens.Space12)
                    .size(width = 36.dp, height = 4.dp)
                    .background(BedduuColors.TextDisabled, CircleShape)
            )
        },
        content = { content() }
    )
}

@Composable
fun ChapterRow(
    title: String,
    subtitle: String,
    isRead: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BedduuShapes.Small)
            .clickable(onClick = onClick)
            .padding(horizontal = BedduuDimens.Space12, vertical = BedduuDimens.Space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            tint = if (isRead) BedduuColors.TextDisabled else BedduuColors.Primary
        )
        Column(Modifier.weight(1f).padding(start = BedduuDimens.Space12)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (isRead) BedduuColors.TextDisabled else BedduuColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = BedduuColors.TextSecondary)
        }
        if (!isRead) BedduuBadge("NEW", isNew = true)
    }
}

@Composable
fun Modifier.bedduuShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "bedduu-shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = MotionSpec.tween(MotionSpec.Long),
            repeatMode = RepeatMode.Restart
        ),
        label = "bedduu-shimmer-phase"
    )
    return drawWithCache {
        val brush = Brush.linearGradient(
            colors = listOf(BedduuColors.SurfaceVariant, BedduuColors.Outline, BedduuColors.SurfaceVariant),
            start = Offset(size.width * (phase - 1f), 0f),
            end = Offset(size.width * phase, size.height)
        )
        onDrawBehind { drawRect(brush) }
    }
}

@Composable
fun CoverPlaceholder(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .clip(BedduuShapes.Cover)
            .bedduuShimmer()
    )
}

@Composable
fun RowPlaceholder(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(48.dp).clip(BedduuShapes.Small).bedduuShimmer())
        Column(Modifier.weight(1f).padding(start = BedduuDimens.Space12)) {
            Spacer(Modifier.fillMaxWidth(0.7f).height(16.dp).clip(CircleShape).bedduuShimmer())
            Spacer(Modifier.height(BedduuDimens.Space8))
            Spacer(Modifier.fillMaxWidth(0.45f).height(12.dp).clip(CircleShape).bedduuShimmer())
        }
    }
}

@Composable
fun BedduuEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(BedduuDimens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BedduuDimens.Space8)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = BedduuColors.TextPrimary)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = BedduuColors.TextSecondary)
    }
}

@Composable
fun BedduuErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(BedduuDimens.Space24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BedduuDimens.Space8)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = BedduuColors.Error)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = BedduuColors.TextSecondary)
        action?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121117, heightDp = 1500)
@Composable
fun BedduuDesignSystemPreview() {
    var query by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(true) }
    var sheetVisible by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.65f) }

    BedduuTheme {
        Column(
            Modifier
                .fillMaxSize()
                .background(BedduuColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(BedduuDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BedduuDimens.Space16)
        ) {
            Text("Bedduu design system", style = MaterialTheme.typography.displayLarge, color = BedduuColors.TextPrimary)
            BedduuCard(Modifier.fillMaxWidth()) {
                Text("Surface card", style = MaterialTheme.typography.titleMedium)
            }
            CoverCard(
                coverUrl = null,
                title = "Manga cover title shown on two lines",
                modifier = Modifier.width(150.dp),
                badge = { BedduuBadge("NEW", Modifier.align(Alignment.TopEnd).padding(8.dp), isNew = true) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BedduuDimens.Space8)) {
                BedduuPrimaryButton("Primary", {})
                BedduuSecondaryButton("Secondary", {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BedduuDimens.Space8)) {
                BedduuTextButton("Text action", {})
                BedduuIconButton({}, contentDescription = "Save") {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = BedduuColors.TextPrimary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(BedduuDimens.Space8)) {
                BedduuChip("Fantasy", true, {})
                BedduuChip("Adventure", false, {})
                BedduuBadge("12")
                BedduuBadge("NEW", isNew = true)
            }
            BedduuTextField(query, { query = it }, "Text field")
            BedduuSearchBar(query, { query = it })
            BedduuSecondaryButton("Open sample sheet", { sheetVisible = true })
            BedduuSwitch(enabled, { enabled = it })
            BedduuSlider(progress, { progress = it })
            BedduuProgress(progress)
            ChapterRow("Chapter 12", "Updated yesterday", isRead = false)
            ChapterRow("Chapter 11", "Read", isRead = true)
            CoverPlaceholder(Modifier.width(120.dp))
            RowPlaceholder()
            BedduuEmptyState("Nothing saved yet", "Your saved manga will appear here.")
            BedduuErrorState("Could not load", "Check your connection and retry.") {
                BedduuPrimaryButton("Retry", {})
            }
            CircularProgressIndicator(color = BedduuColors.Primary)
            if (sheetVisible) {
                BedduuBottomSheet(onDismissRequest = { sheetVisible = false }) {
                    Text(
                        "Sample bottom sheet",
                        modifier = Modifier.padding(BedduuDimens.Space24),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

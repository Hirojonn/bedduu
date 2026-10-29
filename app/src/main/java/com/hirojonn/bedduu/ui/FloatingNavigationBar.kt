package com.hirojonn.bedduu.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.hirojonn.bedduu.ui.theme.LocalIsDarkMode
import com.hirojonn.bedduu.ui.theme.PrimaryColor
import com.hirojonn.bedduu.ui.theme.PrimaryLight
import com.hirojonn.bedduu.ui.theme.TextMuted

data class NavItem(val label: String, val icon: ImageVector)

val mainNavigationItems = listOf(
    NavItem("Browse", Icons.Outlined.Search),
    NavItem("Library", Icons.Outlined.Book),
    NavItem("Profile", Icons.Outlined.PersonOutline),
    NavItem("Settings", Icons.Outlined.Settings)
)

@Composable
fun FloatingNavBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val selected = selectedIndex.coerceIn(items.indices)
    val isDarkMode = LocalIsDarkMode.current
    val selectedColor = if (isDarkMode) PrimaryLight else PrimaryColor
    val unselectedColor = TextMuted
    val pillColor = if (isDarkMode) Color(0xD91B252D) else Color(0xD9FFFFFF)
    val density = LocalDensity.current
    val blobLeft = remember { Animatable(0f) }
    val blobWidth = remember { Animatable(0f) }
    var previousSelection by remember { mutableIntStateOf(selected) }
    var previousTabWidth by remember { mutableFloatStateOf(0f) }
    var positioned by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pillColor, CircleShape)
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            val tabWidth = maxWidth / items.size
            val tabWidthPx = with(density) { tabWidth.toPx() }

            LaunchedEffect(selected, tabWidthPx) {
                if (tabWidthPx <= 0f) return@LaunchedEffect
                val targetLeft = selected * tabWidthPx
                if (!positioned || previousTabWidth != tabWidthPx) {
                    blobLeft.snapTo(targetLeft)
                    blobWidth.snapTo(tabWidthPx)
                    previousTabWidth = tabWidthPx
                    previousSelection = selected
                    positioned = true
                } else if (previousSelection != selected) {
                    val oldLeft = blobLeft.value
                    val oldRight = oldLeft + blobWidth.value
                    val spanLeft = min(oldLeft, targetLeft)
                    val spanRight = max(oldRight, targetLeft + tabWidthPx)
                    coroutineScope {
                        launch { blobLeft.animateTo(spanLeft, tween(150)) }
                        launch { blobWidth.animateTo(spanRight - spanLeft, tween(150)) }
                    }
                    coroutineScope {
                        launch { blobLeft.animateTo(targetLeft, tween(150)) }
                        launch { blobWidth.animateTo(tabWidthPx, tween(150)) }
                    }
                    previousSelection = selected
                }
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(blobLeft.value.roundToInt(), 0)
                    }
                    .width(with(density) { blobWidth.value.toDp() })
                    .height(maxHeight)
                    .background(selectedColor.copy(alpha = if (isDarkMode) 0.18f else 0.12f), CircleShape)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selected == index
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) selectedColor else unselectedColor,
                        animationSpec = spring(),
                        label = "nav-icon-color"
                    )
                    val iconScale = remember { Animatable(1f) }
                    var composed by remember { mutableStateOf(false) }

                    LaunchedEffect(isSelected) {
                        if (isSelected && composed) {
                            iconScale.snapTo(0.8f)
                            iconScale.animateTo(1.18f, tween(100))
                            iconScale.animateTo(1f, tween(250))
                        } else {
                            iconScale.snapTo(1f)
                        }
                        composed = true
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .selectable(
                                selected = isSelected,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onSelect(index) }
                            )
                            .semantics { contentDescription = item.label },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                }
                        )
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(110)) + expandHorizontally(tween(220)),
                            exit = fadeOut(tween(100)) + shrinkHorizontally(tween(220))
                        ) {
                            Text(
                                text = item.label,
                                color = tint,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun FloatingNavBarPreview() {
    FloatingNavBar(
        items = mainNavigationItems,
        selectedIndex = 0,
        onSelect = {}
    )
}

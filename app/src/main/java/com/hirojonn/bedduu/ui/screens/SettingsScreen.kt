package com.hirojonn.bedduu.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.hirojonn.bedduu.ui.designsystem.BedduuDisplayFontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onOpenAbout: () -> Unit,
    scrollToTopSignal: Int = 0,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showResetStatsDialog   by remember { mutableStateOf(false) }
    var showClearBookmarksDialog by remember { mutableStateOf(false) }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) scrollState.animateScrollTo(0)
    }

    // ── Dialogs ─────────────────────────────────────────────────────────────
    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("Reset Statistics?", color = TextMain) },
            text = { Text("This will reset your chapters read, reading time, and streaks back to zero.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetStatistics()
                    showResetStatsDialog = false
                    Toast.makeText(context, "Statistics reset to zero", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reset", color = RedAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceColor
        )
    }

    if (showClearBookmarksDialog) {
        AlertDialog(
            onDismissRequest = { showClearBookmarksDialog = false },
            title = { Text("Clear All Bookmarks?", color = TextMain) },
            text = { Text("This will delete all saved manga from your Library.", color = TextMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllBookmarks()
                    showClearBookmarksDialog = false
                    Toast.makeText(context, "Library bookmarks cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Delete All", color = RedAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearBookmarksDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceColor
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BedduuTokens.ScreenBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(bottom = 130.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to profile", tint = TextMain)
            }
        }
        Text(
            "MAKE IT YOURS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryLight,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp)
        )
        Text(
            "Settings",
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextMain,
            fontFamily = BedduuDisplayFontFamily,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 18.dp)
        )

        // ─── Appearance ───────────────────────────────────────────────────
        SectionHeader("Appearance")

        SettingToggle(
            title = "Dark Mode",
            subtitle = if (viewModel.isDarkMode) "Dark theme enabled" else "Light theme enabled",
            checked = viewModel.isDarkMode
        ) {
            viewModel.isDarkMode = it
            viewModel.savePreferences()
        }

        SettingToggle(
            title = "Pure OLED Black",
            subtitle = "Deep true black in dark mode & reader for OLED screens",
            checked = viewModel.pureBlackOled,
            enabled = viewModel.isDarkMode
        ) {
            viewModel.pureBlackOled = it
            viewModel.savePreferences()
        }

        Spacer(Modifier.height(20.dp))

        // ─── Reader Preferences ───────────────────────────────────────────
        SectionHeader("Reader")

        // Reading Mode Chips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(BedduuTokens.ShapeMedium)
                .background(BedduuTokens.GlassSurface)
                .border(1.dp, BedduuTokens.GlassBorder, BedduuTokens.ShapeMedium)
                .padding(16.dp)
        ) {
            Text("Default Reading Mode", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text("Format for manga panel layout", fontSize = 12.sp, color = TextMuted)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    "webtoon" to "Webtoon (Vertical)",
                    "rtl" to "Right-to-Left (Manga)",
                    "ltr" to "Left-to-Right"
                )
                modes.forEach { (key, label) ->
                    val isSelected = viewModel.readingMode == key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.readingMode = key
                            viewModel.readLeftToRight = (key == "ltr")
                            viewModel.savePreferences()
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryColor,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceVariant,
                            labelColor = TextMuted
                        )
                    )
                }
            }
        }

        SettingToggle(
            title = "Page Numbers",
            subtitle = "Show current panel/chapter counter overlay in reader",
            checked = viewModel.showPageNumber
        ) {
            viewModel.showPageNumber = it
            viewModel.savePreferences()
        }

        SettingToggle(
            title = "Keep Screen On",
            subtitle = "Prevent screen from timing out while reading",
            checked = viewModel.keepScreenOn
        ) {
            viewModel.keepScreenOn = it
            viewModel.savePreferences()
        }

        SettingToggle(
            title = "High Quality Panels",
            subtitle = "Load full resolution manga images from source",
            checked = viewModel.highQualityImages
        ) {
            viewModel.highQualityImages = it
            viewModel.savePreferences()
        }

        Spacer(Modifier.height(20.dp))

        // ─── Library & Automation ─────────────────────────────────────────
        SectionHeader("Library & Sync")

        SettingToggle(
            title = "Auto-Bookmark on Read",
            subtitle = "Automatically add manga to Library when you start reading",
            checked = viewModel.autoBookmark
        ) {
            viewModel.autoBookmark = it
            viewModel.savePreferences()
        }

        SettingToggle(
            title = "Chapter Notifications",
            subtitle = "Alerts for new chapters and library updates",
            checked = viewModel.notificationsEnabled
        ) {
            viewModel.notificationsEnabled = it
            viewModel.savePreferences()
        }

        Spacer(Modifier.height(20.dp))

        // ─── Data & Storage Actions ───────────────────────────────────────
        SectionHeader("Data & Storage")

        SettingActionRow(
            icon = Icons.Default.RestartAlt,
            title = "Reset Statistics",
            subtitle = "${viewModel.chaptersRead} chapters · ${viewModel.readingTimeHours}h read",
            actionLabel = "Reset",
            onClick = { showResetStatsDialog = true }
        )

        SettingActionRow(
            icon = Icons.Default.CleaningServices,
            title = "Clear App Cache",
            subtitle = "Clean temporary files and cached web pages",
            actionLabel = "Clean",
            onClick = {
                viewModel.clearCache(context)
                Toast.makeText(context, "Cache cleaned successfully!", Toast.LENGTH_SHORT).show()
            }
        )

        SettingActionRow(
            icon = Icons.Default.BookmarkRemove,
            title = "Clear All Bookmarks",
            subtitle = "${viewModel.bookmarkedManga.size} manga currently saved",
            actionLabel = "Delete",
            onClick = { showClearBookmarksDialog = true }
        )

        Spacer(Modifier.height(20.dp))

        // ─── About ────────────────────────────────────────────────────────
        SectionHeader("About")

        SettingActionRow(
            icon = Icons.Default.Info,
            title = "About Bedduu",
            subtitle = "Terms of use and manga sources",
            actionLabel = "Open",
            onClick = onOpenAbout
        )

        Spacer(Modifier.height(120.dp))
    }
}

private fun openWebPage(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, "No browser is available to open this link.", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryLight,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 8.dp)
    )
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(BedduuTokens.ShapeMedium)
            .background(BedduuTokens.GlassSurface)
            .border(1.dp, BedduuTokens.GlassBorder, BedduuTokens.ShapeMedium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) TextMain else TextSubtle)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryColor,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceVariant
            )
        )
    }
}

@Composable
fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(BedduuTokens.ShapeMedium)
            .background(BedduuTokens.GlassSurface)
            .border(1.dp, BedduuTokens.GlassBorder, BedduuTokens.ShapeMedium)
            .bedduuSpringClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Text(
            actionLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (actionLabel == "Clear" || actionLabel == "Delete" || actionLabel == "Reset") RedAccent else PrimaryLight
        )
    }
}

@Composable
fun SettingInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BedduuTokens.GlassSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 15.sp, color = TextMain)
        Text(value, fontSize = 13.sp, color = TextMuted)
    }
}

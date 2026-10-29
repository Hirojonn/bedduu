package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    scrollToTopSignal: Int = 0
) {
    val scrollState = rememberScrollState()
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(viewModel.username) }
    var editAvatarIndex by remember { mutableStateOf(viewModel.userAvatarIndex) }

    val currentAvatar = GLOBAL_AVATARS.getOrElse(viewModel.userAvatarIndex) { GLOBAL_AVATARS[0] }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) scrollState.animateScrollTo(0)
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Reader Nickname") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Select Avatar", fontSize = 13.sp, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GLOBAL_AVATARS.forEachIndexed { idx, opt ->
                            val isSel = idx == editAvatarIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(BedduuTokens.ShapeSmall)
                                    .clickable { editAvatarIndex = idx }
                                    .padding(2.dp)
                            ) {
                                BedduuAvatar(
                                    avatarIndex = idx,
                                    size = 40.dp,
                                    showBorder = isSel
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    opt.name.split(" ").first(),
                                    fontSize = 9.sp,
                                    color = if (isSel) PrimaryLight else TextMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editAvatarIndex)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
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
        Text(
            "READER SPACE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryLight,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp)
        )
        Text(
            "Your profile",
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextMain,
            modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 16.dp)
        )

        // ─── User Profile Card ────────────────────────────────────────────
        BedduuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                BedduuAvatar(
                    avatarIndex = viewModel.userAvatarIndex,
                    size = 62.dp
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.username,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        text = "${currentAvatar.name} • Active Reader",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    if (viewModel.currentStreak > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${viewModel.currentStreak} chapter streak",
                                fontSize = 12.sp,
                                color = OrangeAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        editName = viewModel.username
                        editAvatarIndex = viewModel.userAvatarIndex
                        showEditProfileDialog = true
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = PrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ═════════════════════════════════════════════════════════════════════
        // ANALYTICS GRAPH (Weekly Activity Bar Chart & Reading Goal)
        // ═════════════════════════════════════════════════════════════════
        Text(
            text = "Reading Analytics",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))

        BedduuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Graph Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Weekly Activity",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            "Minutes read per day",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    // Total this week badge
                    val totalWeekly = viewModel.weeklyActivity.sum()
                    Surface(
                        shape = CircleShape,
                        color = PrimaryColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            "${totalWeekly}m total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryLight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                // ─── Visual Bar Graph ─────────────────────────────────────
                val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val todayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                val maxMins = (viewModel.weeklyActivity.maxOrNull() ?: 60).coerceAtLeast(40).toFloat()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    viewModel.weeklyActivity.forEachIndexed { index, mins ->
                        val isToday = index == todayIndex
                        val barHeightFactor = (mins / maxMins).coerceIn(0.10f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (mins > 0) "${mins}m" else "-",
                                fontSize = 10.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) PrimaryLight else TextSubtle
                            )

                            Spacer(Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(barHeightFactor)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                    .background(
                                        if (isToday) {
                                            Brush.verticalGradient(
                                                listOf(PrimaryLight, PrimaryColor)
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(
                                                    PrimaryColor.copy(alpha = 0.6f),
                                                    PrimaryColor.copy(alpha = 0.22f)
                                                )
                                            )
                                        }
                                    )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = dayLabels[index],
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) PrimaryLight else TextMuted
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Divider(color = BedduuTokens.BorderSubtle, thickness = 0.5.dp)
                Spacer(Modifier.height(14.dp))

                // ─── Daily Reading Goal Progress Bar ──────────────────────
                val todayMins = viewModel.weeklyActivity.getOrElse(todayIndex) { 0 }
                val goalMins = viewModel.dailyGoalMinutes
                val goalProgress = (todayMins.toFloat() / goalMins.toFloat()).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today's Goal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain
                    )
                    Text(
                        "$todayMins / ${goalMins} mins (${(goalProgress * 100).toInt()}%)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (goalProgress >= 1f) GreenAccent else PrimaryLight
                    )
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = goalProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (goalProgress >= 1f) GreenAccent else PrimaryLight,
                    trackColor = SurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ─── Stats Grid (NO EMOJIS, PURE VECTOR METRICS) ──────────────────
        Text(
            "Overall Statistics",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BedduuStatBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AutoStories,
                label = "Chapters",
                value = viewModel.chaptersRead.toString(),
                valueColor = PrimaryLight
            )
            val readHrs = viewModel.readingTimeMinutes / 60
            val readMins = viewModel.readingTimeMinutes % 60
            BedduuStatBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = "Read Time",
                value = if (readHrs > 0) "${readHrs}h ${readMins}m" else "${readMins}m",
                valueColor = GreenAccent
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BedduuStatBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                label = "Streak",
                value = "${viewModel.currentStreak} days",
                valueColor = OrangeAccent
            )
            BedduuStatBox(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bookmark,
                label = "Bookmarks",
                value = viewModel.bookmarkedManga.size.toString(),
                valueColor = PrimaryLight
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

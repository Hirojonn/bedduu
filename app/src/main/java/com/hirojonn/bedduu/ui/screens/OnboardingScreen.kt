package com.hirojonn.bedduu.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hirojonn.bedduu.R
import com.hirojonn.bedduu.ui.AppViewModel
import com.hirojonn.bedduu.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: AppViewModel,
    onFinished: () -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var selectedAvatarIndex by remember { mutableStateOf(0) }
    var selectedReadingMode by remember { mutableStateOf("webtoon") }
    var pureBlackOled by remember { mutableStateOf(false) }
    var highQuality by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Bendduu App Branding: Official Elf Peek Logo ──────────────────
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .border(2.5.dp, PrimaryLight, CircleShape)
                .background(Color(0xFF141622)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.elf_peek),
                contentDescription = "Bendduu Elf Peek Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Welcome to bedduu",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain
        )

        Text(
            text = "Set up your manga profile and reader preferences",
            fontSize = 13.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // ── 1. Username Input ─────────────────────────────────────────────
        BedduuCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Reader Nickname",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    placeholder = { Text("e.g. MangaOtaku, ShadowReader", color = TextSubtle) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) },
                    singleLine = true,
                    shape = BedduuTokens.ShapeSmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BedduuTokens.BorderSubtle,
                        focusedContainerColor = BackgroundColor,
                        unfocusedContainerColor = BackgroundColor,
                        cursorColor = PrimaryColor,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 2. Choose Avatar (Zero Emojis, Pure Anime Vector Archetypes) ───
        BedduuCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Choose Profile Avatar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GLOBAL_AVATARS.forEachIndexed { index, option ->
                        val isSelected = index == selectedAvatarIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(BedduuTokens.ShapeSmall)
                                .clickable { selectedAvatarIndex = index }
                                .padding(2.dp)
                        ) {
                            BedduuAvatar(
                                avatarIndex = index,
                                size = 44.dp,
                                showBorder = isSelected
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                option.name.split(" ").first(),
                                fontSize = 10.sp,
                                color = if (isSelected) PrimaryLight else TextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 3. Reader Preferences ─────────────────────────────────────────
        BedduuCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Reader Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                )
                Spacer(Modifier.height(12.dp))

                // Reading direction
                Text("Reading Layout", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val modes = listOf("webtoon" to "Webtoon (Vertical)", "manga" to "Manga (Right-to-Left)")
                    modes.forEach { (mode, label) ->
                        val isSel = selectedReadingMode == mode
                        Surface(
                            shape = BedduuTokens.ShapeSmall,
                            color = if (isSel) PrimaryColor else BackgroundColor,
                            modifier = Modifier
                                .weight(1f)
                                .clip(BedduuTokens.ShapeSmall)
                                .border(
                                    1.dp,
                                    if (isSel) PrimaryColor else BedduuTokens.BorderSubtle,
                                    BedduuTokens.ShapeSmall
                                )
                                .clickable { selectedReadingMode = mode }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Divider(color = BedduuTokens.BorderSubtle, thickness = 0.5.dp)
                Spacer(Modifier.height(14.dp))

                // Pure OLED Black Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pure OLED Black", fontSize = 13.sp, color = TextMain)
                        Text("True black background for OLED screens", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = pureBlackOled,
                        onCheckedChange = { pureBlackOled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryColor
                        )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // High Quality Images Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("High Quality Images", fontSize = 13.sp, color = TextMain)
                        Text("Load crisp full resolution panels", fontSize = 11.sp, color = TextMuted)
                    }
                    Switch(
                        checked = highQuality,
                        onCheckedChange = { highQuality = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryColor
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Submit Button ─────────────────────────────────────────────────
        Button(
            onClick = {
                viewModel.completeOnboarding(
                    name = usernameInput,
                    avatar = selectedAvatarIndex,
                    mode = selectedReadingMode,
                    oled = pureBlackOled,
                    quality = highQuality
                )
                onFinished()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
            shape = BedduuTokens.ShapeMedium
        ) {
            Text(
                "Start Reading",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

package com.hirojonn.bedduu.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hirojonn.bedduu.BuildConfig
import com.hirojonn.bedduu.R
import com.hirojonn.bedduu.ui.CustomScraperSource
import com.hirojonn.bedduu.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BedduuTokens.ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain)
            }
            Text(
                text = "About Bedduu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            BedduuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.elf_peek),
                        contentDescription = "Bedduu logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, PrimaryLight, CircleShape)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "bedduu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMain
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            AboutSectionTitle("Terms of use")
            BedduuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.about_description),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = TextMain
                    )
                    Text(
                        text = stringResource(R.string.about_terms_body),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextMuted
                    )
                }
            }

            AboutSectionTitle("Sources")
            Text(
                text = stringResource(R.string.about_sources_description),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextMuted
            )
            CustomScraperSource.values().forEach { source ->
                SettingActionRow(
                    icon = Icons.Default.Language,
                    title = source.prettyName,
                    subtitle = source.domain,
                    actionLabel = "Open",
                    onClick = {
                        openExternalUrl(context, "https://${source.domain}")
                    }
                )
            }

            AboutSectionTitle("Credits")
            SettingActionRow(
                icon = Icons.Default.Code,
                title = "Hirojonn",
                subtitle = "Project author",
                actionLabel = "Open",
                onClick = {
                    openExternalUrl(context, "https://github.com/Hirojonn")
                }
            )
            Text(
                text = stringResource(R.string.about_font_credit),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = PrimaryLight,
        letterSpacing = 1.2.sp
    )
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, "No browser is available to open this link.", Toast.LENGTH_LONG).show()
    }
}

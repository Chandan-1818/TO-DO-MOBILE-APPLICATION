package com.chandan.master.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.chandan.master.R
import com.chandan.master.presentation.components.BrandWordmark
import com.chandan.master.presentation.components.PrimaryButton
import com.chandan.master.presentation.theme.MasterPalette
import com.chandan.master.presentation.theme.Spacing

/** First launch. Always charcoal (like the splash) because the artwork is black ink on parchment. */
@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit) {
    val charcoal = Color(MasterPalette.dark.background)
    Column(
        Modifier.fillMaxSize().background(charcoal).verticalScroll(rememberScrollState()).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Image(painterResource(R.drawable.samurai_artwork), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(colorStops = arrayOf(0f to charcoal, 0.14f to Color.Transparent, 0.6f to Color.Transparent, 1f to charcoal)),
                ),
            )
        }
        BrandWordmark(nameColor = Color(MasterPalette.dark.onBackground), taglineColor = Color(MasterPalette.dark.onSurfaceVariant))
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.welcome_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(MasterPalette.dark.onSurface),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.welcome_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(MasterPalette.dark.onSurfaceVariant),
                textAlign = TextAlign.Center,
            )
            PrimaryButton(stringResource(R.string.welcome_create_account), onClick = onCreateAccount, modifier = Modifier.fillMaxWidth())
        }
    }
}

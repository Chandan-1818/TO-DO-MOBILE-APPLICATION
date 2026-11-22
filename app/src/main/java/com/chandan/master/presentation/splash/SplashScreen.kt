package com.chandan.master.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.chandan.master.R
import com.chandan.master.presentation.components.BrandWordmark
import com.chandan.master.presentation.theme.MasterPalette
import kotlinx.coroutines.delay

/** Short branded hold (not a loading delay: no work happens here). Keep it brief. */
internal const val SPLASH_HOLD_MILLIS = 800L

/** Cinematic splash. Always charcoal, in both light and dark themes, because the artwork is black ink. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val charcoal = Color(MasterPalette.dark.background)
    val parchmentText = Color(MasterPalette.dark.onBackground)
    val mutedText = Color(MasterPalette.dark.onSurfaceVariant)
    val currentOnFinished by rememberUpdatedState(onFinished)
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(SPLASH_HOLD_MILLIS)
        currentOnFinished()
    }

    Box(Modifier.fillMaxSize().background(charcoal)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(450))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        // Decorative artwork: the wordmark below names the app.
                        Image(
                            painter = painterResource(R.drawable.samurai_artwork),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        // Fade the parchment artwork into the charcoal at top and bottom.
                        Box(
                            Modifier.matchParentSize().background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to charcoal,
                                        0.16f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1f to charcoal,
                                    ),
                                ),
                            ),
                        )
                    }
                    BrandWordmark(
                        modifier = Modifier.padding(top = 4.dp),
                        nameColor = parchmentText,
                        taglineColor = mutedText,
                    )
                }
            }
        }
    }
}

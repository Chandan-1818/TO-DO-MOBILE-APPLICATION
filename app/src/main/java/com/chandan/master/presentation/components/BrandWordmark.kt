package com.chandan.master.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chandan.master.R
import com.chandan.master.presentation.theme.MasterWordmarkStyle

@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    nameColor: Color = MaterialTheme.colorScheme.onBackground,
    taglineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.app_name).uppercase(), style = MasterWordmarkStyle, color = nameColor, textAlign = TextAlign.Center)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = taglineColor, textAlign = TextAlign.Center)
    }
}

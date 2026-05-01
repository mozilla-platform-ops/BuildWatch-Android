package com.mozilla.buildwatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mozilla.buildwatch.model.PlatformGroup
import com.mozilla.buildwatch.ui.theme.ColorNeutral
import com.mozilla.buildwatch.ui.theme.toColor

private val DOT_SIZE = 8.dp
private val DOT_SPACING = 3.dp
private const val PLACEHOLDER_COUNT = 3

@Composable
fun PlatformStatusDots(
    platformGroups: List<PlatformGroup>,
    jobsLoaded: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DOT_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!jobsLoaded) {
            repeat(PLACEHOLDER_COUNT) {
                StatusDot(color = ColorNeutral)
            }
        } else {
            platformGroups.forEach { group ->
                StatusDot(color = group.overallStatus.toColor())
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(color),
    )
}

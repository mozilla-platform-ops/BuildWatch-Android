package com.mozilla.buildwatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozilla.buildwatch.model.PlatformGroup
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.ui.theme.ColorSuccess
import com.mozilla.buildwatch.util.shortTimeAgo

private val ColorWatchedBell = Color(0xFFFF9800)
private val ColorFailurePill = Color(0xFFFF4F5E)

@Composable
fun PushRowItem(
    push: Push,
    platformGroups: List<PlatformGroup>,
    jobsLoaded: Boolean,
    failureCount: Int,
    isRunning: Boolean,
    isWatched: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = push.displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = push.authorHandle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isWatched) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Watched",
                    modifier = Modifier.size(14.dp),
                    tint = ColorWatchedBell,
                )
            }
            PlatformStatusDots(
                platformGroups = platformGroups,
                jobsLoaded = jobsLoaded,
            )
            when {
                failureCount > 0 -> FailureBadge(count = failureCount)
                isRunning -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                jobsLoaded && platformGroups.isNotEmpty() -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Passing",
                    modifier = Modifier.size(14.dp),
                    tint = ColorSuccess,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = push.date.shortTimeAgo(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FailureBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(color = ColorFailurePill, shape = RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

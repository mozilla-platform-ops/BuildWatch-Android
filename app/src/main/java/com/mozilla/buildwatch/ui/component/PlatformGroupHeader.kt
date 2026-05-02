package com.mozilla.buildwatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mozilla.buildwatch.model.PlatformGroup
import com.mozilla.buildwatch.ui.theme.ColorNeutral
import com.mozilla.buildwatch.ui.theme.toColor
import com.mozilla.buildwatch.ui.theme.toIcon

@Composable
fun PlatformGroupHeader(
    group: PlatformGroup,
    modifier: Modifier = Modifier,
) {
    val status = group.overallStatus
    val statusColor = status.toColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (status == PlatformGroup.OverallStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = statusColor,
            )
        } else {
            Icon(
                imageVector = status.toIcon(),
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(16.dp),
            )
        }

        Text(
            text = group.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )

        if (group.failureCount > 0) {
            Text(
                text = "${group.failureCount}✗",
                style = MaterialTheme.typography.labelSmall,
                color = com.mozilla.buildwatch.ui.theme.ColorTestFailed,
            )
        }
        if (group.runningCount > 0) {
            Text(
                text = "${group.runningCount}⚙",
                style = MaterialTheme.typography.labelSmall,
                color = com.mozilla.buildwatch.ui.theme.ColorRunning,
            )
        }
        if (group.successCount > 0) {
            Text(
                text = "${group.successCount}✓",
                style = MaterialTheme.typography.labelSmall,
                color = com.mozilla.buildwatch.ui.theme.ColorSuccess,
            )
        }
        if (group.pendingCount > 0) {
            Text(
                text = "${group.pendingCount}⏱",
                style = MaterialTheme.typography.labelSmall,
                color = ColorNeutral,
            )
        }
    }
}

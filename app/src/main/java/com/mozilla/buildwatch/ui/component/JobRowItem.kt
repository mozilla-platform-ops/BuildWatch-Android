package com.mozilla.buildwatch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.JobState
import com.mozilla.buildwatch.ui.theme.ColorNeutral
import com.mozilla.buildwatch.ui.theme.ColorRunning
import com.mozilla.buildwatch.ui.theme.toColor
import com.mozilla.buildwatch.ui.theme.toIcon
import com.mozilla.buildwatch.util.openUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobRowItem(
    job: Job,
    onRetrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showConfirmDialog = true
            }
            false
        },
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Retrigger Job") },
            text = { Text(job.jobTypeName) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onRetrigger()
                }) { Text("Retrigger") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorRunning)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("Retrigger", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        modifier = modifier,
    ) {
        val context = LocalContext.current
        ListItem(
            headlineContent = {
                Text(
                    text = job.jobTypeName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = job.durationString?.let { dur ->
                { Text(dur, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            leadingContent = {
                when {
                    job.isPending -> Icon(
                        Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        tint = ColorNeutral,
                        modifier = Modifier.size(20.dp),
                    )
                    job.isRunning -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ColorRunning,
                    )
                    else -> Icon(
                        job.result.toIcon(),
                        contentDescription = null,
                        tint = job.result.toColor(),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (job.result.isFailure && job.state == JobState.COMPLETED) {
                        Text(
                            text = job.result.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = job.result.toColor(),
                        )
                    }
                    if (job.taskId != null) {
                        IconButton(
                            onClick = {
                                openUrl(context, "https://firefox-ci-tc.services.mozilla.com/tasks/${job.taskId}")
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in Taskcluster",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            },
        )
        HorizontalDivider()
    }
}

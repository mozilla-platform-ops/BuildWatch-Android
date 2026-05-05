package com.mozilla.buildwatch.ui.screen

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.JobResult
import com.mozilla.buildwatch.model.JobState
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.model.PushRevision
import com.mozilla.buildwatch.ui.component.JobRowItem
import com.mozilla.buildwatch.ui.component.PlatformGroupHeader
import com.mozilla.buildwatch.ui.theme.ColorSuccess
import com.mozilla.buildwatch.util.openUrl
import com.mozilla.buildwatch.util.shortTimeAgo
import com.mozilla.buildwatch.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

private val ColorFailurePill = Color(0xFFFF4F5E)
private val ColorBugChip = Color(0xFF1565C0)

private enum class JobFilter(val label: String) {
    ALL("All"),
    FAILURES("Failures"),
    RUNNING("Running"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushDetailScreen(
    pushId: Int,
    onBack: () -> Unit,
    vm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val pushes by vm.pushes.collectAsStateWithLifecycle()
    val jobsByPush by vm.jobsByPush.collectAsStateWithLifecycle()
    val push = pushes.firstOrNull { it.id == pushId }
    val jobs = jobsByPush[pushId] ?: emptyList()
    val platformGroups = if (push != null) vm.platformGroups(push) else emptyList()
    val scope = rememberCoroutineScope()

    LaunchedEffect(push) { push?.let { vm.fetchJobs(it) } }

    var showFailureSheet by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(JobFilter.ALL) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val filteredGroups = when (activeFilter) {
        JobFilter.ALL -> platformGroups
        JobFilter.FAILURES -> platformGroups.mapNotNull { group ->
            val filtered = group.jobs.filter { it.result.isFailure && it.state == JobState.COMPLETED }
            if (filtered.isEmpty()) null else group.copy(jobs = filtered)
        }
        JobFilter.RUNNING -> platformGroups.mapNotNull { group ->
            val filtered = group.jobs.filter { it.state == JobState.RUNNING || it.state == JobState.PENDING }
            if (filtered.isEmpty()) null else group.copy(jobs = filtered)
        }
    }

    if (showFailureSheet && push != null) {
        FailureSummarySheet(
            push = push,
            onDismiss = { showFailureSheet = false },
            vm = vm,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(push?.displayTitle ?: "Push #$pushId", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (platformGroups.isNotEmpty()) {
                        IconButton(onClick = { filterMenuExpanded = true }) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "Filter jobs",
                                tint = if (activeFilter != JobFilter.ALL)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = filterMenuExpanded,
                            onDismissRequest = { filterMenuExpanded = false },
                        ) {
                            JobFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            filter.label,
                                            fontWeight = if (filter == activeFilter)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        activeFilter = filter
                                        filterMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (push == null) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item { AuthorHeader(push) }
            if (jobs.isNotEmpty()) {
                item { PushSummaryBar(jobs) }
            }
            item {
                QuickActionsRow(
                    push = push,
                    failureCount = vm.failureCount(push),
                    onRetriggerAllFailed = { vm.retriggerAllFailed(push) },
                    onFailureSummary = { showFailureSheet = true },
                )
            }
            item {
                Text(
                    text = "Revisions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(push.revisions, key = { it.revision }) { rev ->
                RevisionRow(rev)
                HorizontalDivider()
            }

            if (platformGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "Jobs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                for (group in filteredGroups) {
                    item(key = "header-${group.id}") {
                        PlatformGroupHeader(group = group)
                    }
                    items(group.jobs, key = { "job-${it.id}" }) { job ->
                        JobRowItem(
                            job = job,
                            onRetrigger = { scope.launch { vm.retrigger(job) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorHeader(push: Push) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = push.authorHandle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = push.date.shortTimeAgo(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PushSummaryBar(jobs: List<Job>) {
    val success = jobs.count { it.result == JobResult.SUCCESS && it.state == JobState.COMPLETED }
    val failures = jobs.count { it.result.isFailure && it.state == JobState.COMPLETED }
    val running = jobs.count { it.state == JobState.RUNNING }
    val pending = jobs.count { it.state == JobState.PENDING }
    val total = jobs.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SummaryCell(icon = { Icon(Icons.Filled.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(16.dp)) }, count = success)
        SummaryCell(icon = { Icon(Icons.Filled.Error, null, tint = ColorFailurePill, modifier = Modifier.size(16.dp)) }, count = failures)
        SummaryCell(icon = { Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) }, count = running)
        SummaryCell(icon = { Icon(Icons.Filled.HourglassEmpty, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) }, count = pending)
        Spacer(Modifier.weight(1f))
        Text(
            text = "$total total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCell(icon: @Composable () -> Unit, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon()
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun RevisionRow(rev: PushRevision) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = rev.shortMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rev.revision.take(12),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            rev.bugNumber?.let { bugNum ->
                BugChip(
                    bugNumber = bugNum,
                    onClick = {
                        openUrl(context, "https://bugzilla.mozilla.org/show_bug.cgi?id=$bugNum")
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    push: Push,
    failureCount: Int,
    onRetriggerAllFailed: () -> Unit,
    onFailureSummary: () -> Unit,
) {
    val context = LocalContext.current
    val bugNumber = push.revisions.firstNotNullOfOrNull { it.bugNumber }
    val treeherderUrl = "https://treeherder.mozilla.org/#/jobs?repo=try&revision=${push.revision}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = {}, enabled = false) {
            Text("Retrigger All Failed")
        }
        OutlinedButton(onClick = onFailureSummary, enabled = failureCount > 0) {
            Text("Failure Summary")
        }
        OutlinedButton(onClick = { openUrl(context, treeherderUrl) }) {
            Text("Open in TreeHerder")
        }
        if (bugNumber != null) {
            OutlinedButton(onClick = { openUrl(context, "https://bugzilla.mozilla.org/show_bug.cgi?id=$bugNumber") }) {
                Text("Bug $bugNumber")
            }
        }
    }
}

@Composable
private fun BugChip(bugNumber: String, onClick: () -> Unit) {
    Text(
        text = "Bug $bugNumber",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .background(color = ColorBugChip, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

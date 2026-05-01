package com.mozilla.buildwatch.ui.screen

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.ui.component.PushRowItem
import com.mozilla.buildwatch.util.shortTimeAgo
import com.mozilla.buildwatch.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryPushesScreen(
    onPushClick: (pushId: Int) -> Unit,
    vm: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(
            LocalContext.current.applicationContext as Application,
        ),
    ),
) {
    val pushes by vm.pushes.collectAsStateWithLifecycle()
    val jobsByPush by vm.jobsByPush.collectAsStateWithLifecycle()
    val watchedPushIds by vm.watchedPushIds.collectAsStateWithLifecycle()
    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val lastRefresh by vm.lastRefresh.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.refresh() }

    val titleText = username.substringBefore("@").ifEmpty { "Try Pushes" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                actions = {
                    lastRefresh?.let {
                        Text(
                            text = it.shortTimeAgo(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                isRefreshing && pushes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && pushes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Failed to load", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                pushes.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No try pushes found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(pushes, key = { it.id }) { push ->
                            val groups = vm.platformGroups(push)
                            PushRowItem(
                                push = push,
                                platformGroups = groups,
                                jobsLoaded = jobsByPush.containsKey(push.id),
                                failureCount = vm.failureCount(push),
                                isRunning = vm.isRunning(push),
                                isWatched = push.id in watchedPushIds,
                                onClick = { onPushClick(push.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}


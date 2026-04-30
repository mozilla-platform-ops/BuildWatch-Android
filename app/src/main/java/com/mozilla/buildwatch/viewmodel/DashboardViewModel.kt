package com.mozilla.buildwatch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mozilla.buildwatch.model.FailureGroup
import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.JobState
import com.mozilla.buildwatch.model.PlatformGroup
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.model.TextLogError
import com.mozilla.buildwatch.repository.SettingsRepository
import com.mozilla.buildwatch.service.TreeHerderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    internal val repo = SettingsRepository(application)
    internal val service = TreeHerderService.create()

    private val _pushes = MutableStateFlow<List<Push>>(emptyList())
    val pushes: StateFlow<List<Push>> = _pushes.asStateFlow()

    private val _jobsByPush = MutableStateFlow<Map<Int, List<Job>>>(emptyMap())
    val jobsByPush: StateFlow<Map<Int, List<Job>>> = _jobsByPush.asStateFlow()

    private val _failureLinesByPush = MutableStateFlow<Map<Int, List<TextLogError>>>(emptyMap())
    val failureLinesByPush: StateFlow<Map<Int, List<TextLogError>>> = _failureLinesByPush.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastRefresh = MutableStateFlow<Instant?>(null)
    val lastRefresh: StateFlow<Instant?> = _lastRefresh.asStateFlow()

    val watchedPushIds: StateFlow<Set<Int>> = repo.watchedPushIds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    val username: StateFlow<String> = repo.username.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )

    // In-memory dedup guard — not persisted (matches iOS behaviour)
    internal val notifiedPushIds = mutableSetOf<Int>()

    // ── Computed helpers ──────────────────────────────────────────────────────

    fun platformGroups(push: Push): List<PlatformGroup> {
        val jobs = _jobsByPush.value[push.id] ?: return emptyList()
        return jobs
            .filter { it.tier == 1 }
            .groupBy { "${it.platform}-${it.platformOption}" }
            .map { (_, group) ->
                PlatformGroup(
                    platform = group.first().platform,
                    option = group.first().platformOption,
                    jobs = group,
                )
            }
            .sortedBy { it.displayName }
    }

    fun failureCount(push: Push): Int =
        (_jobsByPush.value[push.id] ?: emptyList())
            .count { it.result.isFailure && it.state == JobState.COMPLETED }

    fun isRunning(push: Push): Boolean =
        (_jobsByPush.value[push.id] ?: emptyList())
            .any { it.state == JobState.RUNNING || it.state == JobState.PENDING }

    fun failureGroups(push: Push): List<FailureGroup> {
        val errors = _failureLinesByPush.value[push.id] ?: return emptyList()
        val byKey = mutableMapOf<String, Pair<MutableSet<Int>, String>>()
        for (error in errors) {
            val key = error.groupKey
            val entry = byKey.getOrPut(key) { mutableSetOf<Int>() to error.line }
            entry.first.add(error.job)
        }
        return byKey.map { (key, pair) ->
            FailureGroup(
                id = key,
                pattern = key,
                affectedJobCount = pair.first.size,
                exampleLine = pair.second,
            )
        }.sortedByDescending { it.affectedJobCount }
    }

    // ── Action stubs (implemented in subsequent issues) ───────────────────────

    fun refresh() { /* bwa-je0.2 */ }

    fun fetchJobs(push: Push) { /* bwa-je0.2 */ }

    fun fetchFailureLines(push: Push) { /* bwa-je0.4 */ }

    fun toggleWatch(push: Push) { /* bwa-je0.3 */ }

    suspend fun retrigger(job: Job) { /* bwa-v0o.3 */ }
}

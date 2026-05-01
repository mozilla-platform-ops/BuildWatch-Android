package com.mozilla.buildwatch.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mozilla.buildwatch.model.FailureGroup
import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.JobState
import com.mozilla.buildwatch.model.PlatformGroup
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.model.TextLogError
import com.mozilla.buildwatch.repository.DataStoreSettingsRepository
import com.mozilla.buildwatch.repository.SettingsRepository
import com.mozilla.buildwatch.service.TreeHerder
import com.mozilla.buildwatch.service.TreeHerderService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

class DashboardViewModel(
    internal val repo: SettingsRepository,
    internal val service: TreeHerder,
) : ViewModel() {

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    repo = DataStoreSettingsRepository(application),
                    service = TreeHerderService.create(),
                )
            }
        }
    }

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

    val notifiedPushIds: StateFlow<Set<Int>> = repo.notifiedPushIds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    val username: StateFlow<String> = repo.username.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )

    private val _completionEvents = MutableSharedFlow<Push>(extraBufferCapacity = 16)
    val completionEvents: SharedFlow<Push> = _completionEvents.asSharedFlow()

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

    // ── Actions ───────────────────────────────────────────────────────────────

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val author = username.value.ifEmpty { null }
                val fetched = service.fetchPushes(count = 20, author = author)
                _pushes.value = fetched
                _lastRefresh.value = Instant.now()
                val watched = watchedPushIds.value
                if (watched.isNotEmpty()) {
                    _jobsByPush.value = _jobsByPush.value.filterKeys { it !in watched }
                }
                for (push in fetched.take(5)) {
                    launch { fetchJobsInternal(push) }
                }
                for (push in fetched.drop(5)) {
                    if (push.id in watched) launch { fetchJobsInternal(push) }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun fetchJobs(push: Push) {
        if (_jobsByPush.value.containsKey(push.id)) return
        viewModelScope.launch { fetchJobsInternal(push) }
    }

    private suspend fun fetchJobsInternal(push: Push) {
        if (_jobsByPush.value.containsKey(push.id)) return
        try {
            val jobs = service.fetchJobs(push.id)
            _jobsByPush.value = _jobsByPush.value + (push.id to jobs)
            checkCompletion(push)
        } catch (_: Exception) {}
    }

    fun fetchFailureLines(push: Push) {
        if (_failureLinesByPush.value.containsKey(push.id)) return
        viewModelScope.launch {
            val jobs = _jobsByPush.value[push.id] ?: return@launch
            val targets = jobs
                .filter { it.result.isFailure && it.state == JobState.COMPLETED }
                .take(15)
            if (targets.isEmpty()) return@launch
            try {
                val errors = coroutineScope {
                    targets.map { job -> async { service.fetchTextLogErrors(job.id) } }.awaitAll()
                }.flatten()
                _failureLinesByPush.value = _failureLinesByPush.value + (push.id to errors)
            } catch (_: Exception) {}
        }
    }

    fun toggleWatch(push: Push) {
        viewModelScope.launch {
            val current = watchedPushIds.value
            val updated = if (push.id in current) current - push.id else current + push.id
            repo.setWatchedPushIds(updated)
        }
    }

    internal fun checkCompletion(
        push: Push,
        watched: Set<Int> = watchedPushIds.value,
        notified: Set<Int> = notifiedPushIds.value,
    ) {
        val jobs = _jobsByPush.value[push.id] ?: return
        if (jobs.isEmpty()) return
        if (jobs.any { it.state == JobState.RUNNING || it.state == JobState.PENDING }) return
        if (push.id !in watched) return
        if (push.id in notified) return
        viewModelScope.launch {
            repo.setNotifiedPushIds(notifiedPushIds.value + push.id)
            _completionEvents.emit(push)
        }
    }

    suspend fun retrigger(job: Job) {
        service.retriggerJob(job.id)
    }
}

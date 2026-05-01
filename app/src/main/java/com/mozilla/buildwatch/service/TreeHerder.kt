package com.mozilla.buildwatch.service

import com.mozilla.buildwatch.model.Job
import com.mozilla.buildwatch.model.Push
import com.mozilla.buildwatch.model.TextLogError

interface TreeHerder {
    suspend fun fetchPushes(count: Int = 20, author: String? = null): List<Push>
    suspend fun fetchJobs(pushId: Int): List<Job>
    suspend fun fetchTextLogErrors(jobId: Int): List<TextLogError>
    suspend fun retriggerJob(jobId: Int)
}

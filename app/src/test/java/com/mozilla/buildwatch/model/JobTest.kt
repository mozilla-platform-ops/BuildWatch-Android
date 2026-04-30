package com.mozilla.buildwatch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class JobTest {

    private fun job(
        state: JobState = JobState.COMPLETED,
        result: JobResult = JobResult.SUCCESS,
        startTimestamp: Int? = null,
        endTimestamp: Int? = null,
    ) = Job(
        id = 1, pushId = 1, taskId = null,
        platform = "linux64", platformOption = "",
        jobTypeName = "test", jobTypeSymbol = "T",
        jobGroupName = "group", jobGroupSymbol = "G",
        state = state, result = result,
        startTimestamp = startTimestamp, endTimestamp = endTimestamp,
        tier = 1,
    )

    // durationString tests

    @Test fun `durationString returns null when timestamps absent`() {
        assertEquals(null, job().durationString)
    }

    @Test fun `durationString formats seconds only`() {
        assertEquals("45s", job(startTimestamp = 1000, endTimestamp = 1045).durationString)
    }

    @Test fun `durationString formats minutes and seconds`() {
        assertEquals("2m 30s", job(startTimestamp = 1000, endTimestamp = 1150).durationString)
    }

    @Test fun `durationString returns null when end before start`() {
        assertEquals(null, job(startTimestamp = 1100, endTimestamp = 1000).durationString)
    }

    // PlatformGroup.overallStatus tests

    private fun group(jobs: List<Job>) = PlatformGroup("linux64", "", jobs)

    @Test fun `overallStatus is FAILING when any job failed`() {
        val jobs = listOf(
            job(result = JobResult.SUCCESS),
            job(result = JobResult.TEST_FAILED),
        )
        assertEquals(PlatformGroup.OverallStatus.FAILING, group(jobs).overallStatus)
    }

    @Test fun `overallStatus is RUNNING when no failures but running jobs`() {
        val jobs = listOf(
            job(state = JobState.RUNNING, result = JobResult.UNKNOWN),
            job(result = JobResult.SUCCESS),
        )
        assertEquals(PlatformGroup.OverallStatus.RUNNING, group(jobs).overallStatus)
    }

    @Test fun `overallStatus is PENDING when no failures or running`() {
        val jobs = listOf(job(state = JobState.PENDING, result = JobResult.UNKNOWN))
        assertEquals(PlatformGroup.OverallStatus.PENDING, group(jobs).overallStatus)
    }

    @Test fun `overallStatus is PASSING when all succeeded`() {
        val jobs = listOf(job(result = JobResult.SUCCESS), job(result = JobResult.SUCCESS))
        assertEquals(PlatformGroup.OverallStatus.PASSING, group(jobs).overallStatus)
    }

    @Test fun `FAILING takes precedence over RUNNING`() {
        val jobs = listOf(
            job(result = JobResult.BUSTED),
            job(state = JobState.RUNNING, result = JobResult.UNKNOWN),
        )
        assertEquals(PlatformGroup.OverallStatus.FAILING, group(jobs).overallStatus)
    }
}

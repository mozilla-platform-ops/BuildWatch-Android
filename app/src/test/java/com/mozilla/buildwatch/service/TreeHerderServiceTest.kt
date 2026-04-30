package com.mozilla.buildwatch.service

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TreeHerderServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: TreeHerderService

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        service = TreeHerderService.create(server.url("/").toString())
    }

    @After fun tearDown() { server.shutdown() }

    // ── fetchPushes ──────────────────────────────────────────────────────────

    @Test fun `fetchPushes sends correct URL without author`() = runTest {
        server.enqueue(MockResponse().setBody(PUSHES_FIXTURE))
        service.fetchPushes(count = 5)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        val path = request.requestUrl!!
        assertEquals("project/try/push/", path.encodedPath.trimStart('/'))
        assertEquals("5", path.queryParameter("count"))
        assertNull(path.queryParameter("author"))
    }

    @Test fun `fetchPushes sends author query param when provided`() = runTest {
        server.enqueue(MockResponse().setBody(PUSHES_FIXTURE))
        service.fetchPushes(count = 20, author = "rcurran@mozilla.com")
        val path = server.takeRequest().requestUrl!!
        assertEquals("rcurran@mozilla.com", path.queryParameter("author"))
        assertEquals("20", path.queryParameter("count"))
    }

    @Test fun `fetchPushes deserializes push fields correctly`() = runTest {
        server.enqueue(MockResponse().setBody(PUSHES_FIXTURE))
        val pushes = service.fetchPushes()
        assertEquals(1, pushes.size)
        val push = pushes[0]
        assertEquals(123456, push.id)
        assertEquals("abcdef1234567890", push.revision)
        assertEquals("rcurran@mozilla.com", push.author)
        assertEquals(1714000000, push.pushTimestamp)
        assertEquals(1, push.revisions.size)
        assertEquals("Bug 1234567 - Fix the thing", push.revisions[0].shortMessage)
        assertEquals("1234567", push.revisions[0].bugNumber)
    }

    // ── fetchJobs ────────────────────────────────────────────────────────────

    @Test fun `fetchJobs sends correct URL and query params`() = runTest {
        server.enqueue(MockResponse().setBody(JOBS_FIXTURE))
        service.fetchJobs(pushId = 123456)
        val path = server.takeRequest().requestUrl!!
        assertEquals("project/try/jobs/", path.encodedPath.trimStart('/'))
        assertEquals("123456", path.queryParameter("push_id"))
        assertEquals("2000", path.queryParameter("count"))
        assertEquals("list", path.queryParameter("return_type"))
        assertEquals("false", path.queryParameter("exclusion_profile"))
    }

    @Test fun `fetchJobs parses compact response end-to-end`() = runTest {
        server.enqueue(MockResponse().setBody(JOBS_FIXTURE))
        val jobs = service.fetchJobs(pushId = 123456)
        assertEquals(1, jobs.size)
        assertEquals(8675309, jobs[0].id)
        assertEquals(123456, jobs[0].pushId)
        assertEquals(com.mozilla.buildwatch.model.JobResult.TEST_FAILED, jobs[0].result)
    }

    // ── parseCompactJobs ─────────────────────────────────────────────────────

    @Test fun `parseCompactJobs maps rows to Jobs by property index`() {
        val jobs = service.parseCompactJobs(JOBS_FIXTURE)
        assertEquals(1, jobs.size)
        val job = jobs[0]
        assertEquals(8675309, job.id)
        assertEquals(123456, job.pushId)
        assertEquals("XYZtaskid", job.taskId)
        assertEquals("linux64", job.platform)
        assertEquals("opt", job.platformOption)
        assertEquals("mochitest-browser-chrome", job.jobTypeName)
        assertEquals("bc7", job.jobTypeSymbol)
        assertEquals("Browser Chrome", job.jobGroupName)
        assertEquals("bc", job.jobGroupSymbol)
        assertEquals(com.mozilla.buildwatch.model.JobState.COMPLETED, job.state)
        assertEquals(com.mozilla.buildwatch.model.JobResult.TEST_FAILED, job.result)
        assertEquals(1714000100, job.startTimestamp)
        assertEquals(1714001200, job.endTimestamp)
        assertEquals(1, job.tier)
    }

    @Test fun `parseCompactJobs falls back to push_id when result_set_id absent`() {
        val body = """{"job_property_names":["id","push_id","state","platform"],"results":[[1,99,"completed","linux64"]]}"""
        val jobs = service.parseCompactJobs(body)
        assertEquals(1, jobs.size)
        assertEquals(99, jobs[0].pushId)
    }

    @Test fun `parseCompactJobs returns empty list for malformed body`() {
        assertEquals(emptyList<Any>(), service.parseCompactJobs("{}"))
    }

    @Test fun `parseCompactJobs skips rows missing required fields`() {
        // no "id" field — row should be skipped
        val body = """{"job_property_names":["state","platform"],"results":[["completed","linux64"]]}"""
        assertEquals(emptyList<Any>(), service.parseCompactJobs(body))
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    companion object {
        private val PUSHES_FIXTURE = """
            {
              "results": [{
                "id": 123456,
                "revision": "abcdef1234567890",
                "author": "rcurran@mozilla.com",
                "push_timestamp": 1714000000,
                "revisions": [{
                  "revision": "abcdef1234567890",
                  "author": "rcurran@mozilla.com",
                  "comments": "Bug 1234567 - Fix the thing\n\nReviewer: r=reviewer"
                }]
              }]
            }
        """.trimIndent()

        private val JOBS_FIXTURE = """
            {
              "job_property_names": [
                "id", "result_set_id", "task_id", "platform", "platform_option",
                "job_type_name", "job_type_symbol", "job_group_name", "job_group_symbol",
                "state", "result", "start_timestamp", "end_timestamp", "tier"
              ],
              "results": [
                [8675309, 123456, "XYZtaskid", "linux64", "opt",
                 "mochitest-browser-chrome", "bc7", "Browser Chrome", "bc",
                 "completed", "testfailed", 1714000100, 1714001200, 1]
              ]
            }
        """.trimIndent()
    }
}

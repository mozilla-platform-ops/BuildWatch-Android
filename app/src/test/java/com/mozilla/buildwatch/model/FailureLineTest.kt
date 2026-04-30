package com.mozilla.buildwatch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FailureLineTest {

    private fun error(line: String) = TextLogError(id = 1, line = line, lineNumber = 1, job = 42)

    @Test fun `groupKey extracts test name from TEST-UNEXPECTED line`() {
        val line = "TEST-UNEXPECTED-FAIL | dom/test/foo.html | Expected pass"
        assertEquals("dom/test/foo.html", error(line).groupKey)
    }

    @Test fun `groupKey extracts test name from PROCESS-CRASH line`() {
        val line = "PROCESS-CRASH | dom/workers/test/crash.html | application crashed"
        assertEquals("dom/workers/test/crash.html", error(line).groupKey)
    }

    @Test fun `groupKey falls back to first 100 chars for unrecognised format`() {
        val line = "Some random log output that doesn't match the pipe pattern"
        assertEquals(line.take(100).trim(), error(line).groupKey)
    }

    @Test fun `groupKey truncates long unrecognised lines to 100 chars`() {
        val line = "x".repeat(200)
        assertEquals("x".repeat(100), error(line).groupKey)
    }

    @Test fun `groupKey handles lines with pipe but non-matching prefix`() {
        val line = "INFO | some.test | detail"
        // prefix is "INFO", doesn't contain TEST-UNEXPECTED or PROCESS-CRASH → fallback
        assertEquals(line.take(100).trim(), error(line).groupKey)
    }

    @Test fun `groupKey trims whitespace from extracted test name`() {
        val line = "TEST-UNEXPECTED-FAIL |   spaced/test.html   | details"
        assertEquals("spaced/test.html", error(line).groupKey)
    }
}

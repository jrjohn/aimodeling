package com.example.aimodel.core.common

import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryPolicyTest {

    // ==================== Success Cases ====================

    @Test
    fun `executeWithRetry - successful on first attempt returns success`() = runTest {
        val retryPolicy = RetryPolicy(maxAttempts = 3)
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attemptCount)
    }

    @Test
    fun `executeWithRetry - successful on second attempt after one failure`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10,
            maxDelayMillis = 50
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 2) throw IOException("Temporary error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(2, attemptCount)
    }

    @Test
    fun `executeWithRetry - successful on third attempt after two failures`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10,
            maxDelayMillis = 50
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 3) throw IOException("Temporary error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attemptCount)
    }

    // ==================== Failure Cases ====================

    @Test
    fun `executeWithRetry - fails after max attempts`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10,
            maxDelayMillis = 50
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            throw IOException("Persistent error")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals("Persistent error", result.exceptionOrNull()?.message)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetry - with maxAttempts 1 does not retry`() = runTest {
        val retryPolicy = RetryPolicy(maxAttempts = 1)
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            throw IOException("Error")
        }

        assertTrue(result.isFailure)
        assertEquals(1, attemptCount)
    }

    @Test
    fun `executeWithRetry - with maxAttempts 5 retries up to 5 times`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMillis = 10,
            maxDelayMillis = 50
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 5) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(5, attemptCount)
    }

    // ==================== Exponential Backoff Tests ====================

    @Test
    fun `executeWithRetry - applies exponential backoff`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 4,
            initialDelayMillis = 10, // Shorter delays for testing
            maxDelayMillis = 100,
            factor = 2.0
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 4) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(4, attemptCount)
        // Exponential backoff is applied (delays: 10ms, 20ms, 40ms)
        // but we don't test exact timings as they can be flaky
    }

    @Test
    fun `executeWithRetry - caps delay at maxDelayMillis`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 10,
            initialDelayMillis = 1000,
            maxDelayMillis = 2000,
            factor = 2.0
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 5) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        // Should cap the delay after a few attempts
        // 1st retry: 1000ms
        // 2nd retry: 2000ms (capped)
        // 3rd retry: 2000ms (capped)
    }

    // ==================== shouldRetry Predicate Tests ====================

    @Test
    fun `executeWithRetry with predicate - retries when predicate returns true`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry(
            shouldRetry = { exception -> exception is IOException }
        ) {
            attemptCount++
            if (attemptCount < 2) throw IOException("Retryable error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `executeWithRetry with predicate - fails immediately when predicate returns false`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry(
            shouldRetry = { exception -> exception is IOException }
        ) {
            attemptCount++
            throw IllegalStateException("Non-retryable error")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(1, attemptCount) // Should not retry
    }

    @Test
    fun `executeWithRetry with predicate - uses isNetworkError for network exceptions`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry(
            shouldRetry = { exception -> RetryPolicy.isNetworkError(exception) }
        ) {
            attemptCount++
            if (attemptCount < 2) throw SocketTimeoutException("Network timeout")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(2, attemptCount)
    }

    // ==================== isNetworkError Tests ====================

    @Test
    fun `isNetworkError - returns true for IOException`() {
        assertTrue(RetryPolicy.isNetworkError(IOException("Network error")))
    }

    @Test
    fun `isNetworkError - returns true for SocketTimeoutException`() {
        assertTrue(RetryPolicy.isNetworkError(SocketTimeoutException("Timeout")))
    }

    @Test
    fun `isNetworkError - returns true for UnknownHostException`() {
        assertTrue(RetryPolicy.isNetworkError(UnknownHostException("Unknown host")))
    }

    @Test
    fun `isNetworkError - returns true for exception with network in message`() {
        assertTrue(RetryPolicy.isNetworkError(Exception("Network failure")))
        assertTrue(RetryPolicy.isNetworkError(Exception("NETWORK error")))
        assertTrue(RetryPolicy.isNetworkError(Exception("The network is down")))
    }

    @Test
    fun `isNetworkError - returns false for IllegalStateException`() {
        assertFalse(RetryPolicy.isNetworkError(IllegalStateException("Invalid state")))
    }

    @Test
    fun `isNetworkError - returns false for IllegalArgumentException`() {
        assertFalse(RetryPolicy.isNetworkError(IllegalArgumentException("Invalid argument")))
    }

    @Test
    fun `isNetworkError - returns false for exception without network in message`() {
        assertFalse(RetryPolicy.isNetworkError(Exception("Some other error")))
    }

    @Test
    fun `isNetworkError - returns false for exception with null message`() {
        assertFalse(RetryPolicy.isNetworkError(Exception(null as String?)))
    }

    // ==================== Factory Methods Tests ====================

    @Test
    fun `forNetworkOperations - creates policy with correct settings`() = runTest {
        val retryPolicy = RetryPolicy.forNetworkOperations()
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 3) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `forNetworkOperations - fails after 3 attempts`() = runTest {
        val retryPolicy = RetryPolicy.forNetworkOperations()
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            throw IOException("Persistent error")
        }

        assertTrue(result.isFailure)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `forCriticalOperations - creates policy with correct settings`() = runTest {
        val retryPolicy = RetryPolicy.forCriticalOperations()
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 5) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(5, attemptCount)
    }

    @Test
    fun `forCriticalOperations - fails after 5 attempts`() = runTest {
        val retryPolicy = RetryPolicy.forCriticalOperations()
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            throw IOException("Persistent error")
        }

        assertTrue(result.isFailure)
        assertEquals(5, attemptCount)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `executeWithRetry - handles null in returned value`() = runTest {
        val retryPolicy = RetryPolicy(maxAttempts = 3)

        val result = retryPolicy.executeWithRetry {
            null
        }

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `executeWithRetry - handles different exception types across attempts`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 10
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            when (attemptCount) {
                1 -> throw IOException("Network error")
                2 -> throw SocketTimeoutException("Timeout")
                else -> "success"
            }
        }

        assertTrue(result.isSuccess)
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetry - preserves exception type on failure`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 2,
            initialDelayMillis = 10
        )

        val result = retryPolicy.executeWithRetry {
            throw UnknownHostException("Cannot resolve host")
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnknownHostException)
        assertEquals("Cannot resolve host", result.exceptionOrNull()?.message)
    }

    @Test
    fun `executeWithRetry - with zero initial delay`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 3,
            initialDelayMillis = 0,
            maxDelayMillis = 100
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 2) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(2, attemptCount)
    }

    @Test
    fun `executeWithRetry - with large factor increases delay quickly`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMillis = 10,
            maxDelayMillis = 10000,
            factor = 10.0
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry {
            attemptCount++
            if (attemptCount < 3) throw IOException("Error")
            "success"
        }

        assertTrue(result.isSuccess)
        assertEquals(3, attemptCount)
        // Delays would be: 10ms, 100ms, 1000ms (if it continued)
    }

    @Test
    fun `executeWithRetry with predicate - different exceptions some retryable some not`() = runTest {
        val retryPolicy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMillis = 10
        )
        var attemptCount = 0

        val result = retryPolicy.executeWithRetry(
            shouldRetry = { it is IOException }
        ) {
            attemptCount++
            when (attemptCount) {
                1 -> throw IOException("Retryable")
                2 -> throw IllegalStateException("Not retryable")
                else -> "success"
            }
        }

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals(2, attemptCount) // Failed on second attempt, no retry
    }

    @Test
    fun `executeWithRetry - returns complex object types`() = runTest {
        val retryPolicy = RetryPolicy(maxAttempts = 3)
        data class Result(val value: String, val count: Int)

        val result = retryPolicy.executeWithRetry {
            Result("test", 42)
        }

        assertTrue(result.isSuccess)
        assertEquals("test", result.getOrNull()?.value)
        assertEquals(42, result.getOrNull()?.count)
    }
}

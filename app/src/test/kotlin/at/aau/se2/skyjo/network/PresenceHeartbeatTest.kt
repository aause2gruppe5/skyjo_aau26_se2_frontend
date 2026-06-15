package at.aau.se2.skyjo.network

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceHeartbeatTest {

    @Test
    fun `sends heartbeat immediately and on each interval while authenticated`() = runTest {
        val api = CountingApi()
        val heartbeat = PresenceHeartbeat(api, isAuthenticated = { true }, intervalMs = 1_000L)

        heartbeat.start(backgroundScope)
        advanceTimeBy(2_500L)
        runCurrent()

        assertEquals(3, api.calls) // t=0, t=1000, t=2000
        heartbeat.stop()
    }

    @Test
    fun `does not send heartbeat while unauthenticated`() = runTest {
        val api = CountingApi()
        val heartbeat = PresenceHeartbeat(api, isAuthenticated = { false }, intervalMs = 1_000L)

        heartbeat.start(backgroundScope)
        advanceTimeBy(3_000L)
        runCurrent()

        assertEquals(0, api.calls)
        heartbeat.stop()
    }

    @Test
    fun `swallows heartbeat errors and keeps looping`() = runTest {
        val api = CountingApi(error = RuntimeException("boom"))
        val heartbeat = PresenceHeartbeat(api, isAuthenticated = { true }, intervalMs = 1_000L)

        heartbeat.start(backgroundScope)
        advanceTimeBy(1_500L)
        runCurrent()

        assertTrue("loop must keep pinging after a failure", api.calls >= 2)
        heartbeat.stop()
    }

    @Test
    fun `start is idempotent`() = runTest {
        val api = CountingApi()
        val heartbeat = PresenceHeartbeat(api, isAuthenticated = { true }, intervalMs = 1_000L)

        heartbeat.start(backgroundScope)
        heartbeat.start(backgroundScope)
        advanceTimeBy(500L)
        runCurrent()

        assertEquals(1, api.calls)
        heartbeat.stop()
    }

    @Test
    fun `stop halts further heartbeats`() = runTest {
        val api = CountingApi()
        val heartbeat = PresenceHeartbeat(api, isAuthenticated = { true }, intervalMs = 1_000L)

        heartbeat.start(backgroundScope)
        runCurrent()
        heartbeat.stop()
        advanceTimeBy(5_000L)
        runCurrent()

        assertEquals(1, api.calls)
    }

    private class CountingApi(private val error: Throwable? = null) : SkyjoApi {
        var calls = 0

        override suspend fun heartbeat() {
            calls++
            error?.let { throw it }
        }
    }
}

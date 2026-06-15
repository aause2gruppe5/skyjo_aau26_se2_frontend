package at.aau.se2.skyjo.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodically pings the backend so the user counts as "online" while the app is in the
 * foreground, even when they are not connected to a lobby websocket. A heartbeat is only
 * sent while [isAuthenticated] returns true; the loop keeps running across login/logout so
 * presence resumes automatically once the user signs in.
 */
class PresenceHeartbeat(
    private val api: SkyjoApi,
    private val isAuthenticated: () -> Boolean,
    private val intervalMs: Long = DEFAULT_INTERVAL_MS,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                if (isAuthenticated()) {
                    runCatching { api.heartbeat() }
                }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 20_000L
    }
}

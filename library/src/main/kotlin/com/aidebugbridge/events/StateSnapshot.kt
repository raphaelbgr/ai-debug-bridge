package com.aidebugbridge.events

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Captures state snapshots at each significant event, allowing AI agents
 * to see what the app state was at any point in time.
 */
class StateSnapshot {

    data class Snapshot(
        val timestamp: Long,
        val eventType: String,
        val activityName: String?,
        val fragmentNames: List<String>,
        val focusedViewId: String?,
        val extras: Map<String, String>,
    )

    companion object {
        private const val MAX_SNAPSHOTS = 200
    }

    private val snapshots = ConcurrentLinkedDeque<Snapshot>()

    /**
     * Capture a new state snapshot.
     */
    fun capture(
        eventType: String,
        activityName: String?,
        fragmentNames: List<String> = emptyList(),
        focusedViewId: String? = null,
        extras: Map<String, String> = emptyMap(),
    ) {
        val snapshot = Snapshot(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            activityName = activityName,
            fragmentNames = fragmentNames,
            focusedViewId = focusedViewId,
            extras = extras,
        )

        snapshots.addLast(snapshot)
        while (snapshots.size > MAX_SNAPSHOTS) {
            snapshots.pollFirst()
        }
    }

    /**
     * Get all snapshots, optionally filtered by time range.
     */
    fun getSnapshots(since: Long = 0): List<Snapshot> {
        return snapshots.filter { it.timestamp >= since }
    }

    /**
     * Get the most recent snapshot.
     */
    fun getLatest(): Snapshot? = snapshots.peekLast()
}

package com.aidebugbridge.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Internal event bus that collects all app events and broadcasts
 * them to WebSocket clients and the /events endpoint.
 *
 * Events are kept in a ring buffer for historical queries and
 * streamed via SharedFlow for real-time WebSocket delivery.
 */
class EventBus {

    data class Event(
        val type: String,
        val timestamp: Long,
        val data: Map<String, String>,
        val causedBy: String? = null,
    )

    companion object {
        private const val MAX_EVENTS = 1000
    }

    private val eventBuffer = ConcurrentLinkedDeque<Event>()
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)

    /** SharedFlow for real-time event streaming (WebSocket). */
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /**
     * Emit an event to both the buffer and the live stream.
     */
    fun emit(type: String, data: Map<String, String>, causedBy: String? = null) {
        val event = Event(
            type = type,
            timestamp = System.currentTimeMillis(),
            data = data,
            causedBy = causedBy,
        )

        eventBuffer.addLast(event)

        // Trim buffer if it exceeds max size
        while (eventBuffer.size > MAX_EVENTS) {
            eventBuffer.pollFirst()
        }

        _events.tryEmit(event)
    }

    /**
     * Get recent events from the buffer.
     */
    fun getRecentEvents(): List<Event> = eventBuffer.toList()

    /**
     * Clear all buffered events.
     */
    fun clear() {
        eventBuffer.clear()
    }
}

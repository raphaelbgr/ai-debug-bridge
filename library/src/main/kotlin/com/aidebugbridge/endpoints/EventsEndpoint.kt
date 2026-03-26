package com.aidebugbridge.endpoints

import com.aidebugbridge.events.EventBus
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /events — Returns recent events from the event bus.
 * Supports filtering by type and limiting the count.
 *
 * Query params:
 *   ?type=activity.resumed  — filter by event type
 *   ?limit=50               — max events to return (default 100)
 *   ?since=1703000000000    — events after this timestamp
 */
class EventsEndpoint(private val eventBus: EventBus) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val type = call.parameters["type"]
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 100
            val since = call.parameters["since"]?.toLongOrNull() ?: 0L

            var events = eventBus.getRecentEvents()

            if (type != null) {
                events = events.filter { it.type == type || it.type.startsWith("$type.") }
            }

            if (since > 0) {
                events = events.filter { it.timestamp > since }
            }

            events = events.takeLast(limit)

            call.respond(HttpStatusCode.OK, mapOf(
                "count" to events.size,
                "events" to events.map { event ->
                    mapOf(
                        "type" to event.type,
                        "timestamp" to event.timestamp,
                        "data" to event.data,
                    )
                }
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Events retrieval failed"))
            )
        }
    }
}

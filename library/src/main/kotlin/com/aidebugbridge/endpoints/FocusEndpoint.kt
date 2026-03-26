package com.aidebugbridge.endpoints

import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.focus.FocusGraph
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /focus — Returns the focus graph: which views are focusable,
 * the currently focused view, and DPAD navigation relationships.
 * Essential for Android TV / Fire TV debugging.
 */
class FocusEndpoint(private val discoveryEngine: DiscoveryEngine) {

    private val focusGraph = FocusGraph()

    suspend fun handle(call: ApplicationCall) {
        try {
            val rootView = discoveryEngine.getRootView()
            if (rootView == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active view"))
                return
            }

            val graph = focusGraph.buildFocusGraph(rootView)
            call.respond(HttpStatusCode.OK, graph)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Focus graph failed"))
            )
        }
    }
}

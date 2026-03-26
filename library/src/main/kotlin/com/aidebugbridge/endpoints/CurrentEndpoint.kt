package com.aidebugbridge.endpoints

import com.aidebugbridge.discovery.DiscoveryEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /current — Returns information about the currently visible screen:
 * activity name, visible fragments, and a snapshot of the view tree.
 */
class CurrentEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val screen = discoveryEngine.getCurrentScreen()
            call.respond(HttpStatusCode.OK, screen)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
}

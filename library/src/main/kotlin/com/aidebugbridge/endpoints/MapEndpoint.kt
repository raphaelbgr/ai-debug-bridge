package com.aidebugbridge.endpoints

import com.aidebugbridge.discovery.DiscoveryEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /map — Returns a full map of the application: activities, fragments,
 * navigation graph, view trees, and Compose semantics trees.
 */
class MapEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val appMap = discoveryEngine.buildAppMap()
            call.respond(HttpStatusCode.OK, appMap)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }
}

package com.aidebugbridge.endpoints

import android.content.Context
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

/**
 * GET /state — Read application state (SharedPreferences, Intent extras, etc.)
 * POST /state — Write state values.
 */
class StateEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handleGet(call: ApplicationCall) {
        try {
            val scope = call.parameters["scope"] ?: "activity"
            val key = call.parameters["key"]
            val activity = discoveryEngine.getCurrentActivity()

            if (activity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active activity"))
                return
            }

            val state: Map<String, Any> = when (scope) {
                "activity" -> {
                    val extras = activity.intent?.extras
                    if (extras != null) {
                        extras.keySet().associateWith { k -> extras.get(k)?.toString() ?: "null" }
                    } else {
                        emptyMap()
                    }
                }
                "shared_preferences" -> {
                    val prefsName = call.parameters["name"] ?: "${activity.packageName}_preferences"
                    val prefs = activity.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    prefs.all.mapValues { (_, v) -> v?.toString() ?: "null" }
                }
                else -> mapOf("error" to "Unknown scope: $scope")
            }

            val result = if (key != null) {
                mapOf("key" to key, "value" to (state[key]?.toString() ?: "null"))
            } else {
                state
            }

            call.respond(HttpStatusCode.OK, result)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "State read failed"))
            )
        }
    }

    suspend fun handlePost(call: ApplicationCall) {
        try {
            val request = call.receive<BridgeRequest.StateWrite>()
            val activity = discoveryEngine.getCurrentActivity()

            if (activity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active activity"))
                return
            }

            when (request.scope) {
                BridgeRequest.StateScope.SHARED_PREFERENCES -> {
                    val prefs = activity.getSharedPreferences(
                        "${activity.packageName}_preferences",
                        Context.MODE_PRIVATE
                    )
                    prefs.edit().putString(request.key, request.value).apply()
                }
                else -> {
                    // TODO: Support writing to other state scopes
                }
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "ok",
                "key" to request.key,
                "scope" to request.scope.name,
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "State write failed"))
            )
        }
    }
}

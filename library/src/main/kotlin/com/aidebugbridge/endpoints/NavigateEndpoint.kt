package com.aidebugbridge.endpoints

import android.content.Intent
import android.net.Uri
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * POST /navigate — Navigate to a specific screen in the app.
 * Supports deep links, Navigation Component destinations, explicit intents, and back navigation.
 */
class NavigateEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val request = call.receive<BridgeRequest.Navigate>()
            val activity = discoveryEngine.getCurrentActivity()

            if (activity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active activity"))
                return
            }

            withContext(Dispatchers.Main) {
                when (request.method) {
                    BridgeRequest.NavigateMethod.DEEP_LINK -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(request.destination))
                        request.arguments.forEach { (k, v) -> intent.putExtra(k, v) }
                        activity.startActivity(intent)
                    }

                    BridgeRequest.NavigateMethod.INTENT -> {
                        val targetClass = Class.forName(request.destination)
                        val intent = Intent(activity, targetClass)
                        request.arguments.forEach { (k, v) -> intent.putExtra(k, v) }
                        activity.startActivity(intent)
                    }

                    BridgeRequest.NavigateMethod.NAV_COMPONENT -> {
                        // TODO: Navigate via NavController using destination route/id
                        // navController.navigate(request.destination)
                    }

                    BridgeRequest.NavigateMethod.BACK -> {
                        activity.onBackPressed()
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("status" to "navigated", "destination" to request.destination))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Navigation failed"))
            )
        }
    }
}

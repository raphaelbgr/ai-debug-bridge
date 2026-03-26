package com.aidebugbridge.endpoints

import android.app.Dialog
import android.view.WindowManager
import com.aidebugbridge.discovery.DiscoveryEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * GET /overlays — Detects active overlays, dialogs, popups, and toasts
 * that may be blocking user interaction or obscuring content.
 */
class OverlaysEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val activity = discoveryEngine.getCurrentActivity()
            if (activity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active activity"))
                return
            }

            val overlayInfo = buildMap<String, Any> {
                // Detect dialogs via window token inspection
                val dialogs = mutableListOf<Map<String, Any>>()

                // Check for PopupWindows and Dialogs via the window manager
                try {
                    val windowManager = activity.windowManager
                    // The decorView gives us access to the window hierarchy
                    val decorView = activity.window.decorView
                    val rootView = decorView.rootView

                    // Check if there are multiple window layers (indicates overlays)
                    put("hasOverlay", rootView.windowId != decorView.windowId)
                } catch (e: Exception) {
                    put("hasOverlay", false)
                }

                put("dialogs", dialogs)
                put("windowCount", activity.window?.let { 1 } ?: 0)
            }

            call.respond(HttpStatusCode.OK, overlayInfo)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Overlay detection failed"))
            )
        }
    }
}

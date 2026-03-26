package com.aidebugbridge.endpoints

import android.view.View
import android.view.ViewGroup
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * POST /action — Perform a UI action on a specific view.
 * Views can be targeted by resource ID, text content, or content description.
 */
class ActionEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val request = call.receive<BridgeRequest.Action>()
            val rootView = discoveryEngine.getRootView()

            if (rootView == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active view"))
                return
            }

            val targetView = findView(rootView, request)
            if (targetView == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "View not found"))
                return
            }

            withContext(Dispatchers.Main) {
                when (request.action) {
                    BridgeRequest.ActionType.CLICK -> targetView.performClick()
                    BridgeRequest.ActionType.LONG_CLICK -> targetView.performLongClick()
                    BridgeRequest.ActionType.FOCUS -> targetView.requestFocus()
                    BridgeRequest.ActionType.SCROLL_UP -> targetView.scrollBy(0, -200)
                    BridgeRequest.ActionType.SCROLL_DOWN -> targetView.scrollBy(0, 200)
                    BridgeRequest.ActionType.SCROLL_LEFT -> targetView.scrollBy(-200, 0)
                    BridgeRequest.ActionType.SCROLL_RIGHT -> targetView.scrollBy(200, 0)
                    BridgeRequest.ActionType.SWIPE_UP -> {
                        // TODO: Implement swipe gesture via MotionEvent injection
                    }
                    BridgeRequest.ActionType.SWIPE_DOWN -> {
                        // TODO: Implement swipe gesture via MotionEvent injection
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "ok",
                "action" to request.action.name,
                "view" to targetView.javaClass.simpleName,
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Action failed"))
            )
        }
    }

    private fun findView(root: View, request: BridgeRequest.Action): View? {
        // Try by resource ID name
        request.viewId?.let { idName ->
            val resId = root.resources.getIdentifier(idName, "id", root.context.packageName)
            if (resId != 0) {
                root.findViewById<View>(resId)?.let { return it }
            }
        }

        // Try by text content or content description via tree traversal
        return findViewRecursive(root, request.viewText, request.contentDescription)
    }

    private fun findViewRecursive(
        view: View,
        text: String?,
        contentDesc: String?,
    ): View? {
        // Check content description
        if (contentDesc != null && view.contentDescription?.toString() == contentDesc) {
            return view
        }

        // Check text for TextViews
        if (text != null && view is android.widget.TextView && view.text?.toString() == text) {
            return view
        }

        // Recurse into children
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findViewRecursive(view.getChildAt(i), text, contentDesc)?.let { return it }
            }
        }

        return null
    }
}

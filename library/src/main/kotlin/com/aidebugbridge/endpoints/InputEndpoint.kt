package com.aidebugbridge.endpoints

import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * POST /input — Enter text into an input field.
 * Can target by view ID, supports append mode and IME submit action.
 */
class InputEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val request = call.receive<BridgeRequest.Input>()
            val rootView = discoveryEngine.getRootView()

            if (rootView == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active view"))
                return
            }

            val editText = findEditText(rootView, request.viewId)
            if (editText == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "EditText not found"))
                return
            }

            withContext(Dispatchers.Main) {
                if (request.append) {
                    editText.append(request.text)
                } else {
                    editText.setText(request.text)
                }

                if (request.submit) {
                    editText.onEditorAction(EditorInfo.IME_ACTION_DONE)
                }
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "ok",
                "text" to request.text,
                "viewId" to (request.viewId ?: "focused"),
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Input failed"))
            )
        }
    }

    private fun findEditText(root: View, viewId: String?): EditText? {
        if (viewId != null) {
            val resId = root.resources.getIdentifier(viewId, "id", root.context.packageName)
            if (resId != 0) {
                return root.findViewById(resId)
            }
        }

        // Fall back to currently focused EditText
        val focused = root.findFocus()
        return focused as? EditText
    }
}

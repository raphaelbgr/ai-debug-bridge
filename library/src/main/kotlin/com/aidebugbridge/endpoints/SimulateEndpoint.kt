package com.aidebugbridge.endpoints

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import com.aidebugbridge.discovery.DiscoveryEngine
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * POST /simulate — Simulate touch gestures, DPAD events, and key presses.
 * Essential for Android TV / Fire TV testing where DPAD is the primary input.
 */
class SimulateEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val request = call.receive<BridgeRequest.Simulate>()
            val activity = discoveryEngine.getCurrentActivity()

            if (activity == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active activity"))
                return
            }

            withContext(Dispatchers.Main) {
                when (request.gesture) {
                    BridgeRequest.GestureType.TAP -> {
                        simulateTap(activity.window.decorView, request.x, request.y)
                    }
                    BridgeRequest.GestureType.LONG_PRESS -> {
                        simulateLongPress(activity.window.decorView, request.x, request.y, request.durationMs)
                    }
                    BridgeRequest.GestureType.SWIPE -> {
                        // TODO: Implement swipe from (x,y) to (x2,y2) over durationMs
                    }
                    BridgeRequest.GestureType.DPAD_UP -> injectKeyEvent(activity, KeyEvent.KEYCODE_DPAD_UP)
                    BridgeRequest.GestureType.DPAD_DOWN -> injectKeyEvent(activity, KeyEvent.KEYCODE_DPAD_DOWN)
                    BridgeRequest.GestureType.DPAD_LEFT -> injectKeyEvent(activity, KeyEvent.KEYCODE_DPAD_LEFT)
                    BridgeRequest.GestureType.DPAD_RIGHT -> injectKeyEvent(activity, KeyEvent.KEYCODE_DPAD_RIGHT)
                    BridgeRequest.GestureType.DPAD_CENTER -> injectKeyEvent(activity, KeyEvent.KEYCODE_DPAD_CENTER)
                    BridgeRequest.GestureType.KEY_EVENT -> {
                        request.keyCode?.let { injectKeyEvent(activity, it) }
                    }
                }
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "status" to "ok",
                "gesture" to request.gesture.name,
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Simulation failed"))
            )
        }
    }

    private fun simulateTap(view: android.view.View, x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)

        view.dispatchTouchEvent(downEvent)
        view.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }

    private suspend fun simulateLongPress(view: android.view.View, x: Float, y: Float, durationMs: Long) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        view.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        delay(durationMs)

        val upEvent = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }

    private fun injectKeyEvent(activity: android.app.Activity, keyCode: Int) {
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        activity.dispatchKeyEvent(downEvent)
        activity.dispatchKeyEvent(upEvent)
    }
}

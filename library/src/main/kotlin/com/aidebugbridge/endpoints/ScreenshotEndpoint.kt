package com.aidebugbridge.endpoints

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.util.Base64
import android.view.View
import com.aidebugbridge.discovery.DiscoveryEngine
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * GET /screenshot — Capture a programmatic screenshot of the current screen.
 * Returns the image as base64-encoded PNG data.
 *
 * Query params:
 *   ?quality=80    — JPEG quality (1-100), omit for PNG
 *   ?format=jpeg   — Output format (png or jpeg)
 *   ?scale=0.5     — Scale factor for smaller images
 */
class ScreenshotEndpoint(private val discoveryEngine: DiscoveryEngine) {

    suspend fun handle(call: ApplicationCall) {
        try {
            val format = call.parameters["format"] ?: "png"
            val quality = call.parameters["quality"]?.toIntOrNull() ?: 90
            val scale = call.parameters["scale"]?.toFloatOrNull() ?: 1.0f

            val rootView = discoveryEngine.getRootView()
            if (rootView == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No active view"))
                return
            }

            val base64 = withContext(Dispatchers.Main) {
                captureView(rootView, format, quality, scale)
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "format" to format,
                "width" to (rootView.width * scale).toInt(),
                "height" to (rootView.height * scale).toInt(),
                "base64" to base64,
            ))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (e.message ?: "Screenshot failed"))
            )
        }
    }

    private fun captureView(view: View, format: String, quality: Int, scale: Float): String {
        val width = (view.width * scale).toInt()
        val height = (view.height * scale).toInt()

        if (width <= 0 || height <= 0) {
            throw IllegalStateException("View has no dimensions (${view.width}x${view.height})")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (scale != 1.0f) {
            canvas.scale(scale, scale)
        }

        view.draw(canvas)

        val stream = ByteArrayOutputStream()
        val compressFormat = if (format == "jpeg") {
            Bitmap.CompressFormat.JPEG
        } else {
            Bitmap.CompressFormat.PNG
        }

        bitmap.compress(compressFormat, quality, stream)
        bitmap.recycle()

        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

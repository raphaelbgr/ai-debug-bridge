package com.aidebugbridge.protocol

import kotlinx.serialization.Serializable

/**
 * Response models for the AI Debug Bridge HTTP API.
 */
object BridgeResponse {

    @Serializable
    data class Success(
        val status: String = "ok",
        val data: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class Error(
        val status: String = "error",
        val error: String,
        val code: Int = 500,
    )

    @Serializable
    data class AppMap(
        val activities: List<Map<String, String>>,
        val currentActivity: String,
        val fragments: List<Map<String, String>> = emptyList(),
        val navGraph: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class CurrentScreen(
        val activity: String,
        val title: String = "",
        val fragments: List<Map<String, String>> = emptyList(),
    )

    @Serializable
    data class EventMessage(
        val type: String,
        val timestamp: Long,
        val data: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class FocusMap(
        val currentFocused: String? = null,
        val focusableViews: List<FocusableView> = emptyList(),
    )

    @Serializable
    data class FocusableView(
        val id: String,
        val className: String,
        val bounds: ViewBounds,
        val isFocused: Boolean = false,
        val neighbors: FocusNeighbors = FocusNeighbors(),
    )

    @Serializable
    data class ViewBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    @Serializable
    data class FocusNeighbors(
        val up: String? = null,
        val down: String? = null,
        val left: String? = null,
        val right: String? = null,
    )

    @Serializable
    data class MemoryInfo(
        val heapUsed: Long,
        val heapMax: Long,
        val heapFree: Long,
        val nativeHeap: Long,
        val objectCount: Map<String, Int> = emptyMap(),
    )

    @Serializable
    data class OverlayInfo(
        val overlays: List<Map<String, String>> = emptyList(),
        val dialogs: List<Map<String, String>> = emptyList(),
        val toasts: List<String> = emptyList(),
    )

    @Serializable
    data class ScreenshotResult(
        val format: String = "png",
        val width: Int,
        val height: Int,
        val base64Data: String,
    )
}

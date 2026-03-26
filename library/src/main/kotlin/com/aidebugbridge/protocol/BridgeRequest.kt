package com.aidebugbridge.protocol

import kotlinx.serialization.Serializable

/**
 * Request models for the AI Debug Bridge HTTP API.
 */
object BridgeRequest {

    @Serializable
    data class Navigate(
        val destination: String,
        val arguments: Map<String, String> = emptyMap(),
        val method: NavigateMethod = NavigateMethod.DEEP_LINK,
    )

    @Serializable
    enum class NavigateMethod {
        DEEP_LINK,
        NAV_COMPONENT,
        INTENT,
        BACK,
    }

    @Serializable
    data class Action(
        val viewId: String? = null,
        val viewText: String? = null,
        val contentDescription: String? = null,
        val action: ActionType,
    )

    @Serializable
    enum class ActionType {
        CLICK,
        LONG_CLICK,
        SCROLL_UP,
        SCROLL_DOWN,
        SCROLL_LEFT,
        SCROLL_RIGHT,
        SWIPE_UP,
        SWIPE_DOWN,
        FOCUS,
    }

    @Serializable
    data class Input(
        val viewId: String? = null,
        val text: String,
        val append: Boolean = false,
        val submit: Boolean = false,
    )

    @Serializable
    data class StateWrite(
        val key: String,
        val value: String,
        val scope: StateScope = StateScope.ACTIVITY,
    )

    @Serializable
    enum class StateScope {
        ACTIVITY,
        APPLICATION,
        SHARED_PREFERENCES,
        VIEW_MODEL,
    }

    @Serializable
    data class Simulate(
        val gesture: GestureType,
        val x: Float = 0f,
        val y: Float = 0f,
        val x2: Float = 0f,
        val y2: Float = 0f,
        val durationMs: Long = 300,
        val keyCode: Int? = null,
    )

    @Serializable
    enum class GestureType {
        TAP,
        LONG_PRESS,
        SWIPE,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        DPAD_CENTER,
        KEY_EVENT,
    }

    // ── Bluetooth HID Remote Control ──

    @Serializable
    data class BluetoothConnect(
        val macAddress: String,
    )

    @Serializable
    data class BluetoothSend(
        val type: BluetoothSendType,
        val text: String? = null,
    )

    @Serializable
    enum class BluetoothSendType {
        // DPAD navigation
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        DPAD_CENTER,

        // Media keys
        PLAY_PAUSE,
        VOLUME_UP,
        VOLUME_DOWN,
        MUTE,
        BACK,
        HOME,
        MENU,
        STOP,
        NEXT_TRACK,
        PREV_TRACK,

        // Text input
        TEXT,
    }
}

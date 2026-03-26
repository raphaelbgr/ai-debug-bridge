package com.aidebugbridge.endpoints

import android.os.Build
import com.aidebugbridge.bluetooth.BluetoothDiscovery
import com.aidebugbridge.bluetooth.BluetoothHidRemote
import com.aidebugbridge.bluetooth.HidReportDescriptor
import com.aidebugbridge.protocol.BridgeRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

/**
 * Bluetooth HID remote control endpoints.
 *
 * POST /bluetooth/connect    — Connect to a target device by MAC address
 * POST /bluetooth/disconnect — Disconnect from the current device
 * GET  /bluetooth/status     — Connection state and device info
 * GET  /bluetooth/discover   — Scan for nearby Bluetooth devices
 * POST /bluetooth/send       — Send a navigation, media, or text command
 * GET  /bluetooth/paired     — List paired devices (no scan)
 */
class BluetoothEndpoint(
    private val hidRemote: BluetoothHidRemote?,
    private val discovery: BluetoothDiscovery?,
) {

    suspend fun handleConnect(call: ApplicationCall) {
        val remote = requireRemote(call) ?: return
        try {
            val request = call.receive<BridgeRequest.BluetoothConnect>()

            if (!request.macAddress.matches(Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$"))) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid MAC address format"))
                return
            }

            val success = remote.connect(request.macAddress)
            call.respond(
                if (success) HttpStatusCode.OK else HttpStatusCode.InternalServerError,
                mapOf(
                    "status" to if (success) "connecting" else "failed",
                    "macAddress" to request.macAddress,
                )
            )
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Connect failed")))
        }
    }

    suspend fun handleDisconnect(call: ApplicationCall) {
        val remote = requireRemote(call) ?: return
        try {
            val success = remote.disconnect()
            call.respond(HttpStatusCode.OK, mapOf(
                "status" to if (success) "disconnected" else "not_connected",
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Disconnect failed")))
        }
    }

    suspend fun handleStatus(call: ApplicationCall) {
        val remote = requireRemote(call) ?: return
        try {
            val deviceInfo = remote.getConnectedDeviceInfo()
            call.respond(HttpStatusCode.OK, mapOf(
                "connected" to remote.isConnected(),
                "state" to remote.getConnectionState().name,
                "device" to (deviceInfo?.let {
                    mapOf("name" to it.name, "macAddress" to it.macAddress)
                }),
                "supported" to BluetoothHidRemote.isSupported(),
                "apiLevel" to Build.VERSION.SDK_INT,
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Status failed")))
        }
    }

    suspend fun handleDiscover(call: ApplicationCall) {
        val disc = discovery ?: run {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "Bluetooth not supported on this device (requires API 28+)"
            ))
            return
        }
        try {
            val durationMs = call.request.queryParameters["duration"]?.toLongOrNull() ?: 10_000L
            val tvOnly = call.request.queryParameters["tvOnly"]?.toBooleanStrictOrNull() ?: false

            val devices = disc.discover(durationMs = durationMs, filterTvOnly = tvOnly)
            call.respond(HttpStatusCode.OK, mapOf(
                "devices" to devices.map { d ->
                    mapOf(
                        "name" to d.name,
                        "macAddress" to d.macAddress,
                        "type" to d.type.name,
                        "rssi" to d.rssi,
                        "bonded" to d.bonded,
                        "majorClass" to d.majorClass,
                    )
                },
                "count" to devices.size,
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Discovery failed")))
        }
    }

    suspend fun handlePaired(call: ApplicationCall) {
        val disc = discovery ?: run {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "Bluetooth not supported on this device (requires API 28+)"
            ))
            return
        }
        try {
            val devices = disc.getPairedDevices()
            call.respond(HttpStatusCode.OK, mapOf(
                "devices" to devices.map { d ->
                    mapOf(
                        "name" to d.name,
                        "macAddress" to d.macAddress,
                        "type" to d.type.name,
                        "majorClass" to d.majorClass,
                    )
                },
                "count" to devices.size,
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "List failed")))
        }
    }

    suspend fun handleSend(call: ApplicationCall) {
        val remote = requireRemote(call) ?: return
        try {
            val request = call.receive<BridgeRequest.BluetoothSend>()

            if (!remote.isConnected()) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Not connected to any device"))
                return
            }

            val success = when (request.type) {
                // DPAD navigation
                BridgeRequest.BluetoothSendType.DPAD_UP -> remote.sendDpadKey(HidReportDescriptor.HatSwitch.UP)
                BridgeRequest.BluetoothSendType.DPAD_DOWN -> remote.sendDpadKey(HidReportDescriptor.HatSwitch.DOWN)
                BridgeRequest.BluetoothSendType.DPAD_LEFT -> remote.sendDpadKey(HidReportDescriptor.HatSwitch.LEFT)
                BridgeRequest.BluetoothSendType.DPAD_RIGHT -> remote.sendDpadKey(HidReportDescriptor.HatSwitch.RIGHT)
                BridgeRequest.BluetoothSendType.DPAD_CENTER -> remote.sendCenterKey()

                // Media keys
                BridgeRequest.BluetoothSendType.PLAY_PAUSE -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.PLAY_PAUSE)
                BridgeRequest.BluetoothSendType.VOLUME_UP -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.VOLUME_UP)
                BridgeRequest.BluetoothSendType.VOLUME_DOWN -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.VOLUME_DOWN)
                BridgeRequest.BluetoothSendType.MUTE -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.MUTE)
                BridgeRequest.BluetoothSendType.BACK -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.BACK)
                BridgeRequest.BluetoothSendType.HOME -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.HOME)
                BridgeRequest.BluetoothSendType.MENU -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.MENU)
                BridgeRequest.BluetoothSendType.STOP -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.STOP)
                BridgeRequest.BluetoothSendType.NEXT_TRACK -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.NEXT_TRACK)
                BridgeRequest.BluetoothSendType.PREV_TRACK -> remote.sendMediaKey(HidReportDescriptor.ConsumerUsage.PREV_TRACK)

                // Text input via keyboard emulation
                BridgeRequest.BluetoothSendType.TEXT -> {
                    val text = request.text ?: run {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "text field required for TEXT type"))
                        return
                    }
                    remote.sendText(text)
                }
            }

            call.respond(HttpStatusCode.OK, mapOf(
                "status" to if (success) "sent" else "failed",
                "type" to request.type.name,
                "text" to request.text,
            ))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Send failed")))
        }
    }

    private suspend fun requireRemote(call: ApplicationCall): BluetoothHidRemote? {
        if (hidRemote == null) {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf(
                "error" to "Bluetooth HID not supported (requires API 28+)",
                "apiLevel" to Build.VERSION.SDK_INT,
                "requiredApiLevel" to 28,
            ))
            return null
        }
        return hidRemote
    }
}

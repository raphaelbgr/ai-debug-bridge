package com.aidebugbridge.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import java.util.concurrent.Executor

/**
 * Bluetooth HID Device wrapper that registers the Android device as a
 * Bluetooth remote control. Once paired and connected to a target device
 * (Android TV, Fire TV, Apple TV, etc.), it can send DPAD navigation,
 * media keys, and keyboard input over Bluetooth.
 *
 * Uses the Android BluetoothHidDevice API (API 28+).
 *
 * Architecture:
 *   AI Agent → HTTP/MCP → BluetoothEndpoint → BluetoothHidRemote → BT HID → Target Device
 */
@RequiresApi(Build.VERSION_CODES.P)
class BluetoothHidRemote(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHidRemote"
        private const val SDP_NAME = "AI Debug Bridge Remote"
        private const val SDP_DESCRIPTION = "AI-controlled Bluetooth remote control"
        private const val SDP_PROVIDER = "AI Debug Bridge"
        private const val KEY_PRESS_DURATION_MS = 30L

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    data class DeviceInfo(
        val name: String,
        val macAddress: String,
        val state: ConnectionState,
    )

    interface ConnectionCallback {
        fun onStateChanged(device: BluetoothDevice, state: ConnectionState)
        fun onAppRegistered(registered: Boolean)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var connectionState = ConnectionState.DISCONNECTED
    private var isRegistered = false
    private var callback: ConnectionCallback? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            Log.i(TAG, "App status changed: registered=$registered")
            isRegistered = registered
            callback?.onAppRegistered(registered)
        }

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val newState = when (state) {
                BluetoothProfile.STATE_CONNECTED -> ConnectionState.CONNECTED
                BluetoothProfile.STATE_CONNECTING -> ConnectionState.CONNECTING
                else -> ConnectionState.DISCONNECTED
            }
            Log.i(TAG, "Connection state: ${device.address} -> $newState")
            connectionState = newState
            connectedDevice = if (newState == ConnectionState.CONNECTED) device else null
            callback?.onStateChanged(device, newState)
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            Log.d(TAG, "onGetReport: type=$type id=$id")
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            Log.d(TAG, "onSetReport: type=$type id=$id")
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            Log.d(TAG, "onInterruptData: reportId=$reportId")
        }
    }

    /**
     * Register this device as a Bluetooth HID device.
     * Must be called before connect/send operations.
     */
    @SuppressLint("MissingPermission")
    fun register(connectionCallback: ConnectionCallback? = null): Boolean {
        this.callback = connectionCallback

        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "Bluetooth not available")
            return false
        }

        if (!adapter.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled")
            return false
        }

        val sdp = BluetoothHidDeviceAppSdpSettings(
            SDP_NAME,
            SDP_DESCRIPTION,
            SDP_PROVIDER,
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidReportDescriptor.DESCRIPTOR,
        )

        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,    // token rate
            9,      // token bucket size
            0,      // peak bandwidth
            11250,  // latency (microseconds)
            11250,  // delay variation
        )

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                hidDevice = proxy as BluetoothHidDevice
                Log.i(TAG, "HID Device service connected")

                val executor: Executor = Executor { it.run() }
                hidDevice?.registerApp(sdp, null, qos, executor, hidCallback)
            }

            override fun onServiceDisconnected(profile: Int) {
                Log.w(TAG, "HID Device service disconnected")
                hidDevice = null
                isRegistered = false
            }
        }

        return adapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
    }

    /**
     * Unregister and clean up the HID device.
     */
    @SuppressLint("MissingPermission")
    fun unregister() {
        hidDevice?.let { device ->
            connectedDevice?.let { device.disconnect(it) }
            device.unregisterApp()
        }
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        hidDevice = null
        connectedDevice = null
        connectionState = ConnectionState.DISCONNECTED
        isRegistered = false
        scope.cancel()
        Log.i(TAG, "HID Device unregistered")
    }

    /**
     * Connect to a target device by MAC address.
     * The target must already be paired via Bluetooth settings.
     */
    @SuppressLint("MissingPermission")
    fun connect(macAddress: String): Boolean {
        val hid = hidDevice ?: run {
            Log.e(TAG, "HID device not registered")
            return false
        }

        if (!isRegistered) {
            Log.e(TAG, "App not registered")
            return false
        }

        val device = bluetoothAdapter?.getRemoteDevice(macAddress) ?: run {
            Log.e(TAG, "Device not found: $macAddress")
            return false
        }

        connectionState = ConnectionState.CONNECTING
        return hid.connect(device)
    }

    /**
     * Disconnect from the currently connected device.
     */
    @SuppressLint("MissingPermission")
    fun disconnect(): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false
        return hid.disconnect(device)
    }

    /**
     * Send a DPAD navigation key via the gamepad hat switch.
     *
     * @param direction One of HidReportDescriptor.HatSwitch values
     */
    @SuppressLint("MissingPermission")
    suspend fun sendDpadKey(direction: Int): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false

        // Gamepad report: [reportId, hatSwitch(4bits) + buttons(4bits)]
        // Hat switch is in the lower nibble, buttons in upper nibble
        val hatAndButtons = (direction and 0x0F).toByte()

        // Key down
        val downReport = byteArrayOf(HidReportDescriptor.REPORT_ID_GAMEPAD, hatAndButtons)
        val sent = hid.sendReport(device, HidReportDescriptor.REPORT_ID_GAMEPAD.toInt(), downReport)
        if (!sent) return false

        delay(KEY_PRESS_DURATION_MS)

        // Key up (hat = centered, no buttons)
        val upReport = byteArrayOf(
            HidReportDescriptor.REPORT_ID_GAMEPAD,
            HidReportDescriptor.HatSwitch.CENTERED.toByte()
        )
        return hid.sendReport(device, HidReportDescriptor.REPORT_ID_GAMEPAD.toInt(), upReport)
    }

    /**
     * Send center/select button press (gamepad button 1).
     */
    @SuppressLint("MissingPermission")
    suspend fun sendCenterKey(): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false

        // Hat centered + button 1 pressed
        // Lower nibble = hat (0x0F = centered), upper nibble = buttons (bit 0 = button 1)
        val hatAndButtons = ((1 shl 4) or HidReportDescriptor.HatSwitch.CENTERED).toByte()
        val downReport = byteArrayOf(HidReportDescriptor.REPORT_ID_GAMEPAD, hatAndButtons)
        val sent = hid.sendReport(device, HidReportDescriptor.REPORT_ID_GAMEPAD.toInt(), downReport)
        if (!sent) return false

        delay(KEY_PRESS_DURATION_MS)

        // Release
        val upReport = byteArrayOf(
            HidReportDescriptor.REPORT_ID_GAMEPAD,
            HidReportDescriptor.HatSwitch.CENTERED.toByte()
        )
        return hid.sendReport(device, HidReportDescriptor.REPORT_ID_GAMEPAD.toInt(), upReport)
    }

    /**
     * Send a media/consumer control key.
     *
     * @param usage One of HidReportDescriptor.ConsumerUsage values
     */
    @SuppressLint("MissingPermission")
    suspend fun sendMediaKey(usage: Int): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false

        // Consumer control report: [reportId, usageLow, usageHigh]
        val downReport = byteArrayOf(
            HidReportDescriptor.REPORT_ID_CONSUMER,
            (usage and 0xFF).toByte(),
            ((usage shr 8) and 0xFF).toByte(),
        )
        val sent = hid.sendReport(device, HidReportDescriptor.REPORT_ID_CONSUMER.toInt(), downReport)
        if (!sent) return false

        delay(KEY_PRESS_DURATION_MS)

        // Key up (usage = 0)
        val upReport = byteArrayOf(
            HidReportDescriptor.REPORT_ID_CONSUMER,
            0x00.toByte(),
            0x00.toByte(),
        )
        return hid.sendReport(device, HidReportDescriptor.REPORT_ID_CONSUMER.toInt(), upReport)
    }

    /**
     * Type a string via BT keyboard emulation.
     * Much faster than DPAD-navigating an on-screen keyboard.
     *
     * @param text The string to type
     * @param delayBetweenKeysMs Delay between keystrokes (default 20ms)
     */
    @SuppressLint("MissingPermission")
    suspend fun sendText(text: String, delayBetweenKeysMs: Long = 20): Boolean {
        val hid = hidDevice ?: return false
        val device = connectedDevice ?: return false

        for (char in text) {
            val hidKey = HidReportDescriptor.KeyboardUsage.charToHidKey(char) ?: continue
            val (scanCode, modifier) = hidKey

            // Keyboard report: [reportId, modifier, reserved, key1, key2, key3, key4, key5, key6]
            val downReport = byteArrayOf(
                HidReportDescriptor.REPORT_ID_KEYBOARD,
                modifier.toByte(),
                0x00,                     // reserved
                scanCode.toByte(),        // key 1
                0x00, 0x00, 0x00, 0x00, 0x00, // keys 2-6 (unused)
            )
            val sent = hid.sendReport(device, HidReportDescriptor.REPORT_ID_KEYBOARD.toInt(), downReport)
            if (!sent) return false

            delay(KEY_PRESS_DURATION_MS)

            // Key up
            val upReport = byteArrayOf(
                HidReportDescriptor.REPORT_ID_KEYBOARD,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            )
            hid.sendReport(device, HidReportDescriptor.REPORT_ID_KEYBOARD.toInt(), upReport)

            delay(delayBetweenKeysMs)
        }
        return true
    }

    fun isConnected(): Boolean = connectionState == ConnectionState.CONNECTED

    fun getConnectionState(): ConnectionState = connectionState

    @SuppressLint("MissingPermission")
    fun getConnectedDeviceInfo(): DeviceInfo? {
        val device = connectedDevice ?: return null
        return DeviceInfo(
            name = device.name ?: "Unknown",
            macAddress = device.address,
            state = connectionState,
        )
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<DeviceInfo> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            DeviceInfo(
                name = device.name ?: "Unknown",
                macAddress = device.address,
                state = if (device == connectedDevice) connectionState else ConnectionState.DISCONNECTED,
            )
        } ?: emptyList()
    }
}

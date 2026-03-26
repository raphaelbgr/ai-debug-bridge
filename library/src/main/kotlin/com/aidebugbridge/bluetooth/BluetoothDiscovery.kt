package com.aidebugbridge.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

/**
 * Discovers nearby Bluetooth devices that could be targets for
 * HID remote control connection (TVs, set-top boxes, media players).
 *
 * Supports both Classic Bluetooth discovery and BLE scanning.
 * Filters results by device class to highlight TV/media devices.
 */
class BluetoothDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothDiscovery"
        private const val DEFAULT_SCAN_DURATION_MS = 10_000L
    }

    @Serializable
    data class DiscoveredDevice(
        val name: String,
        val macAddress: String,
        val type: DeviceType,
        val rssi: Int = 0,
        val bonded: Boolean = false,
        val majorClass: String = "Unknown",
    )

    @Serializable
    enum class DeviceType {
        TV,
        SET_TOP_BOX,
        MEDIA_PLAYER,
        AUDIO,
        PHONE,
        COMPUTER,
        PERIPHERAL,
        OTHER,
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val discoveredDevices = mutableMapOf<String, DiscoveredDevice>()
    private var classicReceiver: BroadcastReceiver? = null
    private var isScanning = false

    /**
     * Discover nearby Bluetooth devices.
     * Combines Classic BT discovery with paired devices list.
     *
     * @param durationMs How long to scan (default 10 seconds)
     * @param filterTvOnly If true, only return TV/media device types
     */
    @SuppressLint("MissingPermission")
    suspend fun discover(
        durationMs: Long = DEFAULT_SCAN_DURATION_MS,
        filterTvOnly: Boolean = false,
    ): List<DiscoveredDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()
        discoveredDevices.clear()

        // Add already-paired devices first
        adapter.bondedDevices?.forEach { device ->
            val discovered = classifyDevice(device, rssi = 0, bonded = true)
            discoveredDevices[device.address] = discovered
        }

        // Start Classic BT discovery
        startClassicDiscovery()

        // Wait for scan duration
        delay(durationMs)

        // Stop discovery
        stopDiscovery()

        val results = discoveredDevices.values.toList()
        return if (filterTvOnly) {
            results.filter { it.type in listOf(DeviceType.TV, DeviceType.SET_TOP_BOX, DeviceType.MEDIA_PLAYER) }
        } else {
            results.sortedByDescending { it.rssi }
        }
    }

    /**
     * Get paired devices without scanning.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<DiscoveredDevice> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            classifyDevice(device, rssi = 0, bonded = true)
        } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (isScanning) return

        classicReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        } ?: return

                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0).toInt()
                        val discovered = classifyDevice(device, rssi, bonded = device.bondState == BluetoothDevice.BOND_BONDED)
                        discoveredDevices[device.address] = discovered
                        Log.d(TAG, "Found: ${discovered.name} (${discovered.macAddress}) type=${discovered.type}")
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                        Log.i(TAG, "Classic discovery finished. Found ${discoveredDevices.size} devices")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(classicReceiver, filter)

        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }
        adapter.startDiscovery()
        isScanning = true
        Log.i(TAG, "Classic BT discovery started")
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        classicReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // Already unregistered
            }
        }
        classicReceiver = null
        isScanning = false
    }

    @SuppressLint("MissingPermission")
    private fun classifyDevice(device: BluetoothDevice, rssi: Int, bonded: Boolean): DiscoveredDevice {
        val deviceClass = device.bluetoothClass
        val majorClass = deviceClass?.majorDeviceClass ?: 0

        val type = when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                when (deviceClass?.deviceClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> DeviceType.SET_TOP_BOX
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER -> DeviceType.TV
                    BluetoothClass.Device.AUDIO_VIDEO_VCR -> DeviceType.MEDIA_PLAYER
                    BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO -> DeviceType.AUDIO
                    BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> DeviceType.AUDIO
                    BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> DeviceType.AUDIO
                    else -> DeviceType.AUDIO
                }
            }
            BluetoothClass.Device.Major.PHONE -> DeviceType.PHONE
            BluetoothClass.Device.Major.COMPUTER -> DeviceType.COMPUTER
            BluetoothClass.Device.Major.PERIPHERAL -> DeviceType.PERIPHERAL
            else -> {
                // Heuristic: device name matching for TVs that don't report proper class
                val name = (device.name ?: "").lowercase()
                when {
                    name.contains("fire tv") || name.contains("firetv") -> DeviceType.SET_TOP_BOX
                    name.contains("android tv") || name.contains("google tv") -> DeviceType.TV
                    name.contains("apple tv") || name.contains("appletv") -> DeviceType.SET_TOP_BOX
                    name.contains("roku") -> DeviceType.SET_TOP_BOX
                    name.contains("chromecast") -> DeviceType.MEDIA_PLAYER
                    name.contains("shield") -> DeviceType.SET_TOP_BOX
                    name.contains("mi box") || name.contains("mibox") -> DeviceType.SET_TOP_BOX
                    name.contains("tv") -> DeviceType.TV
                    else -> DeviceType.OTHER
                }
            }
        }

        val majorClassName = when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
            BluetoothClass.Device.Major.NETWORKING -> "Networking"
            BluetoothClass.Device.Major.IMAGING -> "Imaging"
            BluetoothClass.Device.Major.MISC -> "Misc"
            else -> "Unknown ($majorClass)"
        }

        return DiscoveredDevice(
            name = device.name ?: "Unknown",
            macAddress = device.address,
            type = type,
            rssi = rssi,
            bonded = bonded,
            majorClass = majorClassName,
        )
    }
}

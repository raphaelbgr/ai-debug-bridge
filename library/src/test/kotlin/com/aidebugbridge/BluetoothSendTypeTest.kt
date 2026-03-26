package com.aidebugbridge

import com.aidebugbridge.protocol.BridgeRequest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BluetoothSendType enum — validates all command types
 * and their categorization.
 */
class BluetoothSendTypeTest {

    @Test
    fun `all 16 command types exist`() {
        assertEquals(16, BridgeRequest.BluetoothSendType.entries.size)
    }

    @Test
    fun `DPAD types are present`() {
        val dpadTypes = BridgeRequest.BluetoothSendType.entries.filter { it.name.startsWith("DPAD_") }
        assertEquals(5, dpadTypes.size)
        assertTrue(dpadTypes.map { it.name }.containsAll(
            listOf("DPAD_UP", "DPAD_DOWN", "DPAD_LEFT", "DPAD_RIGHT", "DPAD_CENTER")
        ))
    }

    @Test
    fun `media types are present`() {
        val mediaTypes = listOf("PLAY_PAUSE", "VOLUME_UP", "VOLUME_DOWN", "MUTE", "STOP", "NEXT_TRACK", "PREV_TRACK")
        for (type in mediaTypes) {
            assertNotNull("$type should exist", BridgeRequest.BluetoothSendType.valueOf(type))
        }
    }

    @Test
    fun `navigation types are present`() {
        assertNotNull(BridgeRequest.BluetoothSendType.valueOf("BACK"))
        assertNotNull(BridgeRequest.BluetoothSendType.valueOf("HOME"))
        assertNotNull(BridgeRequest.BluetoothSendType.valueOf("MENU"))
    }

    @Test
    fun `TEXT type exists for keyboard input`() {
        assertNotNull(BridgeRequest.BluetoothSendType.valueOf("TEXT"))
    }

    @Test
    fun `BluetoothConnect requires MAC address`() {
        val connect = BridgeRequest.BluetoothConnect(macAddress = "AA:BB:CC:DD:EE:FF")
        assertEquals("AA:BB:CC:DD:EE:FF", connect.macAddress)
    }

    @Test
    fun `BluetoothSend defaults text to null`() {
        val send = BridgeRequest.BluetoothSend(type = BridgeRequest.BluetoothSendType.DPAD_UP)
        assertNull(send.text)
    }

    @Test
    fun `BluetoothSend accepts text for TEXT type`() {
        val send = BridgeRequest.BluetoothSend(
            type = BridgeRequest.BluetoothSendType.TEXT,
            text = "hello world"
        )
        assertEquals("hello world", send.text)
    }
}

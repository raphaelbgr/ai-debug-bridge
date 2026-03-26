package com.aidebugbridge

import com.aidebugbridge.bluetooth.HidReportDescriptor
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for HidReportDescriptor — validates USB HID spec compliance
 * and character-to-scancode mapping.
 */
class HidReportDescriptorTest {

    @Test
    fun `descriptor is not empty`() {
        assertTrue(HidReportDescriptor.DESCRIPTOR.isNotEmpty())
    }

    @Test
    fun `descriptor contains all three report IDs`() {
        val bytes = HidReportDescriptor.DESCRIPTOR
        // Report ID tags are 0x85 followed by the ID
        val reportIds = mutableListOf<Byte>()
        for (i in 0 until bytes.size - 1) {
            if (bytes[i] == 0x85.toByte()) {
                reportIds.add(bytes[i + 1])
            }
        }
        assertTrue("Should contain keyboard report ID", reportIds.contains(HidReportDescriptor.REPORT_ID_KEYBOARD))
        assertTrue("Should contain consumer report ID", reportIds.contains(HidReportDescriptor.REPORT_ID_CONSUMER))
        assertTrue("Should contain gamepad report ID", reportIds.contains(HidReportDescriptor.REPORT_ID_GAMEPAD))
    }

    @Test
    fun `descriptor has matching collection open and close tags`() {
        val bytes = HidReportDescriptor.DESCRIPTOR
        var openCount = 0
        var closeCount = 0
        for (i in bytes.indices) {
            if (bytes[i] == 0xA1.toByte()) openCount++   // Collection (Application)
            if (bytes[i] == 0xC0.toByte()) closeCount++   // End Collection
        }
        assertEquals("Collection open/close count must match", openCount, closeCount)
    }

    @Test
    fun `descriptor starts with Usage Page Generic Desktop for keyboard`() {
        val bytes = HidReportDescriptor.DESCRIPTOR
        // First two bytes should be Usage Page (0x05) Generic Desktop (0x01)
        assertEquals(0x05.toByte(), bytes[0])
        assertEquals(0x01.toByte(), bytes[1])
    }

    @Test
    fun `hat switch values are within range`() {
        assertTrue(HidReportDescriptor.HatSwitch.UP in 0..7)
        assertTrue(HidReportDescriptor.HatSwitch.DOWN in 0..7)
        assertTrue(HidReportDescriptor.HatSwitch.LEFT in 0..7)
        assertTrue(HidReportDescriptor.HatSwitch.RIGHT in 0..7)
        assertEquals(0x0F, HidReportDescriptor.HatSwitch.CENTERED)
    }

    @Test
    fun `hat switch cardinal directions are 90 degrees apart`() {
        assertEquals(0, HidReportDescriptor.HatSwitch.UP)
        assertEquals(2, HidReportDescriptor.HatSwitch.RIGHT)
        assertEquals(4, HidReportDescriptor.HatSwitch.DOWN)
        assertEquals(6, HidReportDescriptor.HatSwitch.LEFT)
    }

    @Test
    fun `consumer usage values are valid USB HID usage IDs`() {
        assertTrue(HidReportDescriptor.ConsumerUsage.PLAY_PAUSE > 0)
        assertTrue(HidReportDescriptor.ConsumerUsage.VOLUME_UP > 0)
        assertTrue(HidReportDescriptor.ConsumerUsage.VOLUME_DOWN > 0)
        assertTrue(HidReportDescriptor.ConsumerUsage.MUTE > 0)
        assertTrue(HidReportDescriptor.ConsumerUsage.HOME > 0)
        assertTrue(HidReportDescriptor.ConsumerUsage.BACK > 0)
        // All usage IDs should fit in 16 bits (consumer control report size)
        assertTrue(HidReportDescriptor.ConsumerUsage.HOME <= 0xFFFF)
        assertTrue(HidReportDescriptor.ConsumerUsage.BACK <= 0xFFFF)
    }

    @Test
    fun `charToHidKey maps lowercase letters correctly`() {
        val aResult = HidReportDescriptor.KeyboardUsage.charToHidKey('a')
        assertNotNull(aResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_A, aResult!!.first)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_NONE, aResult.second)

        val zResult = HidReportDescriptor.KeyboardUsage.charToHidKey('z')
        assertNotNull(zResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_Z, zResult!!.first)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_NONE, zResult.second)
    }

    @Test
    fun `charToHidKey maps uppercase letters with shift modifier`() {
        val aResult = HidReportDescriptor.KeyboardUsage.charToHidKey('A')
        assertNotNull(aResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_A, aResult!!.first)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_LEFT_SHIFT, aResult.second)
    }

    @Test
    fun `charToHidKey maps digits correctly`() {
        val oneResult = HidReportDescriptor.KeyboardUsage.charToHidKey('1')
        assertNotNull(oneResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_1, oneResult!!.first)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_NONE, oneResult.second)

        val zeroResult = HidReportDescriptor.KeyboardUsage.charToHidKey('0')
        assertNotNull(zeroResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_0, zeroResult!!.first)
    }

    @Test
    fun `charToHidKey maps space and enter`() {
        val spaceResult = HidReportDescriptor.KeyboardUsage.charToHidKey(' ')
        assertNotNull(spaceResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_SPACE, spaceResult!!.first)

        val enterResult = HidReportDescriptor.KeyboardUsage.charToHidKey('\n')
        assertNotNull(enterResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.KEY_ENTER, enterResult!!.first)
    }

    @Test
    fun `charToHidKey maps common symbols`() {
        val dotResult = HidReportDescriptor.KeyboardUsage.charToHidKey('.')
        assertNotNull(dotResult)

        val atResult = HidReportDescriptor.KeyboardUsage.charToHidKey('@')
        assertNotNull(atResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_LEFT_SHIFT, atResult!!.second)

        val questionResult = HidReportDescriptor.KeyboardUsage.charToHidKey('?')
        assertNotNull(questionResult)
        assertEquals(HidReportDescriptor.KeyboardUsage.MOD_LEFT_SHIFT, questionResult!!.second)
    }

    @Test
    fun `charToHidKey returns null for unmapped characters`() {
        // Unicode characters not on a US keyboard
        assertNull(HidReportDescriptor.KeyboardUsage.charToHidKey('€'))
        assertNull(HidReportDescriptor.KeyboardUsage.charToHidKey('ñ'))
    }

    @Test
    fun `all lowercase letters map to sequential scan codes`() {
        var prevScanCode = -1
        for (c in 'a'..'z') {
            val result = HidReportDescriptor.KeyboardUsage.charToHidKey(c)
            assertNotNull("Character '$c' should be mapped", result)
            if (prevScanCode >= 0) {
                assertEquals("Characters should be sequential", prevScanCode + 1, result!!.first)
            }
            prevScanCode = result!!.first
        }
    }

    @Test
    fun `report IDs are unique`() {
        val ids = setOf(
            HidReportDescriptor.REPORT_ID_KEYBOARD,
            HidReportDescriptor.REPORT_ID_CONSUMER,
            HidReportDescriptor.REPORT_ID_GAMEPAD,
        )
        assertEquals("All report IDs must be unique", 3, ids.size)
    }
}

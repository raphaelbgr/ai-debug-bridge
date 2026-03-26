package com.aidebugbridge.bluetooth

/**
 * USB HID Report Descriptors for Bluetooth HID Device registration.
 *
 * Three report types enable full remote control capability:
 * - Report ID 1: Keyboard (text entry via BT keyboard emulation)
 * - Report ID 2: Consumer Control (media keys: play/pause, volume, mute)
 * - Report ID 3: Gamepad/DPAD (directional navigation + center/select)
 *
 * Descriptors follow the USB HID Specification 1.11.
 * Reference: https://www.usb.org/sites/default/files/hid1_11.pdf
 */
object HidReportDescriptor {

    const val REPORT_ID_KEYBOARD: Byte = 0x01
    const val REPORT_ID_CONSUMER: Byte = 0x02
    const val REPORT_ID_GAMEPAD: Byte = 0x03

    /**
     * Combined HID Report Descriptor covering all three report types.
     * This single descriptor is registered with BluetoothHidDevice.
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        // ========================================
        // Report ID 1: Keyboard
        // Usage Page: Keyboard/Keypad (0x07)
        // ========================================
        0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),       // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,  //   Report ID (1)

        // Modifier keys (8 bits: Ctrl, Shift, Alt, GUI x2)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0xE0.toByte(),       //   Usage Minimum (Left Control)
        0x29.toByte(), 0xE7.toByte(),       //   Usage Maximum (Right GUI)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),       //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),       //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),       //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),       //   Input (Data, Variable, Absolute)

        // Reserved byte
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x81.toByte(), 0x01.toByte(),       //   Input (Constant)

        // Key codes (6 simultaneous keys)
        0x05.toByte(), 0x07.toByte(),       //   Usage Page (Keyboard/Keypad)
        0x19.toByte(), 0x00.toByte(),       //   Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(),       //   Usage Maximum (101)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(),       //   Logical Maximum (101)
        0x75.toByte(), 0x08.toByte(),       //   Report Size (8)
        0x95.toByte(), 0x06.toByte(),       //   Report Count (6)
        0x81.toByte(), 0x00.toByte(),       //   Input (Data, Array, Absolute)

        0xC0.toByte(),                       // End Collection

        // ========================================
        // Report ID 2: Consumer Control (Media Keys)
        // Usage Page: Consumer (0x0C)
        // ========================================
        0x05.toByte(), 0x0C.toByte(),       // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(),       // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER,  //   Report ID (2)

        // 16-bit consumer control usage
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x03.toByte(), // Logical Maximum (1023)
        0x19.toByte(), 0x00.toByte(),       //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x03.toByte(), // Usage Maximum (1023)
        0x75.toByte(), 0x10.toByte(),       //   Report Size (16)
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x81.toByte(), 0x00.toByte(),       //   Input (Data, Array, Absolute)

        0xC0.toByte(),                       // End Collection

        // ========================================
        // Report ID 3: Gamepad (DPAD Navigation)
        // Usage Page: Generic Desktop (0x01)
        // ========================================
        0x05.toByte(), 0x01.toByte(),       // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(),       // Usage (Gamepad)
        0xA1.toByte(), 0x01.toByte(),       // Collection (Application)
        0x85.toByte(), REPORT_ID_GAMEPAD,   //   Report ID (3)

        // Hat switch for DPAD (8 directions + null)
        0x05.toByte(), 0x01.toByte(),       //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x39.toByte(),       //   Usage (Hat Switch)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x07.toByte(),       //   Logical Maximum (7)
        0x35.toByte(), 0x00.toByte(),       //   Physical Minimum (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(),       //   Unit (Degrees)
        0x75.toByte(), 0x04.toByte(),       //   Report Size (4)
        0x95.toByte(), 0x01.toByte(),       //   Report Count (1)
        0x81.toByte(), 0x42.toByte(),       //   Input (Data, Variable, Absolute, Null State)

        // Buttons (4 bits for center/select + 3 spare)
        0x05.toByte(), 0x09.toByte(),       //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),       //   Usage Minimum (Button 1 = Center/Select)
        0x29.toByte(), 0x04.toByte(),       //   Usage Maximum (Button 4)
        0x15.toByte(), 0x00.toByte(),       //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),       //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),       //   Report Size (1)
        0x95.toByte(), 0x04.toByte(),       //   Report Count (4)
        0x81.toByte(), 0x02.toByte(),       //   Input (Data, Variable, Absolute)

        0xC0.toByte(),                       // End Collection
    )

    /**
     * Hat switch values for DPAD directions.
     * 0=Up, 1=Up-Right, 2=Right, 3=Down-Right, 4=Down, 5=Down-Left, 6=Left, 7=Up-Left
     * 0x0F (15) = null/centered (no direction pressed)
     */
    object HatSwitch {
        const val UP: Int = 0
        const val UP_RIGHT: Int = 1
        const val RIGHT: Int = 2
        const val DOWN_RIGHT: Int = 3
        const val DOWN: Int = 4
        const val DOWN_LEFT: Int = 5
        const val LEFT: Int = 6
        const val UP_LEFT: Int = 7
        const val CENTERED: Int = 0x0F  // Null state — no direction
    }

    /**
     * Consumer Control usage IDs (USB HID Usage Tables, Usage Page 0x0C).
     */
    object ConsumerUsage {
        const val PLAY_PAUSE: Int = 0xCD
        const val STOP: Int = 0xB7
        const val NEXT_TRACK: Int = 0xB5
        const val PREV_TRACK: Int = 0xB4
        const val VOLUME_UP: Int = 0xE9
        const val VOLUME_DOWN: Int = 0xEA
        const val MUTE: Int = 0xE2
        const val MENU: Int = 0x40   // Menu key
        const val HOME: Int = 0x223  // AC Home
        const val BACK: Int = 0x224  // AC Back
        const val POWER: Int = 0x30  // Power
    }

    /**
     * Keyboard scan codes (USB HID Usage Tables, Usage Page 0x07).
     * Only the most common ones for text entry.
     */
    object KeyboardUsage {
        const val KEY_A: Int = 0x04
        const val KEY_Z: Int = 0x1D
        const val KEY_1: Int = 0x1E
        const val KEY_0: Int = 0x27
        const val KEY_ENTER: Int = 0x28
        const val KEY_ESCAPE: Int = 0x29
        const val KEY_BACKSPACE: Int = 0x2A
        const val KEY_TAB: Int = 0x2B
        const val KEY_SPACE: Int = 0x2C
        const val KEY_MINUS: Int = 0x2D
        const val KEY_EQUALS: Int = 0x2E
        const val KEY_LEFT_BRACKET: Int = 0x2F
        const val KEY_RIGHT_BRACKET: Int = 0x30
        const val KEY_PERIOD: Int = 0x37
        const val KEY_SLASH: Int = 0x38
        const val KEY_COMMA: Int = 0x36
        const val KEY_SEMICOLON: Int = 0x33
        const val KEY_APOSTROPHE: Int = 0x34

        /** Modifier bit flags for the modifier byte */
        const val MOD_NONE: Int = 0x00
        const val MOD_LEFT_SHIFT: Int = 0x02
        const val MOD_LEFT_CTRL: Int = 0x01
        const val MOD_LEFT_ALT: Int = 0x04

        /**
         * Convert a character to its HID keyboard scan code and modifier.
         * Returns Pair(scanCode, modifier) or null if unmappable.
         */
        fun charToHidKey(char: Char): Pair<Int, Int>? {
            return when {
                char in 'a'..'z' -> Pair(KEY_A + (char - 'a'), MOD_NONE)
                char in 'A'..'Z' -> Pair(KEY_A + (char - 'A'), MOD_LEFT_SHIFT)
                char in '1'..'9' -> Pair(KEY_1 + (char - '1'), MOD_NONE)
                char == '0' -> Pair(KEY_0, MOD_NONE)
                char == ' ' -> Pair(KEY_SPACE, MOD_NONE)
                char == '\n' -> Pair(KEY_ENTER, MOD_NONE)
                char == '\t' -> Pair(KEY_TAB, MOD_NONE)
                char == '-' -> Pair(KEY_MINUS, MOD_NONE)
                char == '_' -> Pair(KEY_MINUS, MOD_LEFT_SHIFT)
                char == '=' -> Pair(KEY_EQUALS, MOD_NONE)
                char == '+' -> Pair(KEY_EQUALS, MOD_LEFT_SHIFT)
                char == '.' -> Pair(KEY_PERIOD, MOD_NONE)
                char == ',' -> Pair(KEY_COMMA, MOD_NONE)
                char == '/' -> Pair(KEY_SLASH, MOD_NONE)
                char == '?' -> Pair(KEY_SLASH, MOD_LEFT_SHIFT)
                char == ';' -> Pair(KEY_SEMICOLON, MOD_NONE)
                char == ':' -> Pair(KEY_SEMICOLON, MOD_LEFT_SHIFT)
                char == '\'' -> Pair(KEY_APOSTROPHE, MOD_NONE)
                char == '"' -> Pair(KEY_APOSTROPHE, MOD_LEFT_SHIFT)
                char == '@' -> Pair(KEY_1 + 1, MOD_LEFT_SHIFT) // Shift+2
                char == '!' -> Pair(KEY_1, MOD_LEFT_SHIFT)
                char == '#' -> Pair(KEY_1 + 2, MOD_LEFT_SHIFT) // Shift+3
                char == '$' -> Pair(KEY_1 + 3, MOD_LEFT_SHIFT)
                char == '%' -> Pair(KEY_1 + 4, MOD_LEFT_SHIFT)
                else -> null
            }
        }
    }
}

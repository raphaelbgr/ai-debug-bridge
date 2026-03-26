package com.aidebugbridge.sample

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * A second activity to demonstrate navigation tracking
 * and multi-activity inspection via the debug bridge.
 */
class SecondActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            val clicks = intent.getIntExtra("click_count", 0)
            text = "Second Activity\nReceived click count: $clicks"
            textSize = 20f
            setPadding(32, 32, 32, 32)
        }

        setContentView(textView)
    }
}

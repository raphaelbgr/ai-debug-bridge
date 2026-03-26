package com.aidebugbridge.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * Sample main activity demonstrating a simple screen
 * that the AI Debug Bridge can inspect and interact with.
 */
class MainActivity : Activity() {

    private var clickCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val counterText = findViewById<TextView>(R.id.counter_text)
        val clickButton = findViewById<Button>(R.id.click_button)
        val navigateButton = findViewById<Button>(R.id.navigate_button)

        clickButton.setOnClickListener {
            clickCount++
            counterText.text = "Clicks: $clickCount"
        }

        navigateButton.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java).apply {
                putExtra("click_count", clickCount)
            })
        }
    }
}

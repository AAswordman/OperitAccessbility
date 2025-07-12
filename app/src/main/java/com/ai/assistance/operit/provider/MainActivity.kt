package com.ai.assistance.operit.provider

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ai.assistance.operit.provider.R

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusCheckRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        val goToSettingsButton: Button = findViewById(R.id.go_to_settings_button)

        goToSettingsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        statusCheckRunnable = Runnable {
            updateStatus()
            handler.postDelayed(statusCheckRunnable, 1000) // 每秒检查一次
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(statusCheckRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusCheckRunnable)
    }

    private fun updateStatus() {
        if (UIAccessibilityService.isServiceConnected) {
            statusText.text = "服务状态: 已连接"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_connected))
        } else {
            statusText.text = "服务状态: 未连接"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_disconnected))
        }
    }
} 
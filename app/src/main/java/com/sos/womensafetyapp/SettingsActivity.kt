package com.sos.womensafetyapp

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.SharedPreferences
import com.google.android.material.switchmaterial.SwitchMaterial


class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)
        val themePrefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)

        val isDark = themePrefs.getBoolean("dark_mode", false)

        switchDarkMode.setOnCheckedChangeListener(null)
        switchDarkMode.isChecked = isDark

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->

            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            themePrefs.edit().putBoolean("dark_mode", isChecked).apply()
        }

        val switchRecording = findViewById<SwitchMaterial>(R.id.switchEnableRecording)

// Create SharedPreferences
        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

// Load saved state
        val isRecordingEnabled = prefs.getBoolean("recording_enabled", false)
        switchRecording.isChecked = isRecordingEnabled

// Save when toggled
        switchRecording.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("recording_enabled", isChecked).apply()
        }


        val toolbar = findViewById<Toolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // go back to MainActivity
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
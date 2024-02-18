package com.musalasoft.weatherapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.notifications.WeatherNotificationService

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWeatherNotificationIfNeeded()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showWeatherNotificationIfNeeded() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this) ?: return

        // we should check permission here because user can update Android version between app launches
        val foregroundServicesPermissionGranted =
            isForegroundServicesPermissionGranted
        val isWeatherNotificationEnabled =
            prefs.getBoolean(getString(R.string.settings_enable_notification_key), false)
        if (isWeatherNotificationEnabled && foregroundServicesPermissionGranted) {
            WeatherNotificationService.start(this)
        }
    }

    private val isForegroundServicesPermissionGranted: Boolean
        private get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) true else (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.FOREGROUND_SERVICE
        )
                == PackageManager.PERMISSION_GRANTED) // There is no need for this permission prior Android Pie (Android SDK 28)
}

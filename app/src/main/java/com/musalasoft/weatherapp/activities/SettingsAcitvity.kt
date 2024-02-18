package com.musalasoft.weatherapp.activities

import android.Manifest
import com.musalasoft.weatherapp.R
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.notifications.WeatherNotificationService
import com.musalasoft.weatherapp.utils.UI
import java.text.SimpleDateFormat
import java.util.Date

class SettingsActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {
    // Thursday 2016-01-14 16:00:00
    private val SAMPLE_DATE = Date(1452805200000L)
    public override fun onCreate(savedInstanceState: Bundle?) {
        var theme: Int
        setTheme(
            UI.getTheme(
                PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "fresh")
            ).also {
                theme = it
            })
        val darkTheme = theme == R.style.AppTheme_NoActionBar_Dark ||
                theme == R.style.AppTheme_NoActionBar_Classic_Dark
        val blackTheme = theme == R.style.AppTheme_NoActionBar_Black ||
                theme == R.style.AppTheme_NoActionBar_Classic_Black
        UI.setNavigationBarMode(this@SettingsActivity, darkTheme, blackTheme)
        super.onCreate(savedInstanceState)
        val root = findViewById<View>(android.R.id.list).parent.parent.parent as LinearLayout
        val bar: View = LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false)
        root.addView(bar, 0)
        val toolbar = findViewById<View>(R.id.settings_toolbar) as Toolbar
        toolbar.setNavigationOnClickListener { finish() }
        addPreferencesFromResource(R.xml.prefs)
    }

    public override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        setCustomDateEnabled()
        updateDateFormatList()

        // Set summaries to current value
        setListPreferenceSummary("unit")
        setListPreferenceSummary("lengthUnit")
        setListPreferenceSummary("speedUnit")
        setListPreferenceSummary("pressureUnit")
        setListPreferenceSummary("refreshInterval")
        setListPreferenceSummary("windDirectionFormat")
        setListPreferenceSummary("theme")
        setListPreferenceSummary(getString(R.string.settings_notification_type_key))
    }

    public override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "unit", "lengthUnit", "speedUnit", "pressureUnit", "windDirectionFormat" -> setListPreferenceSummary(
                key
            )

            "refreshInterval" -> {
                setListPreferenceSummary(key)
                AlarmReceiver.setRecurringAlarm(this)
            }

            "dateFormat" -> {
                setCustomDateEnabled()
                setListPreferenceSummary(key)
            }

            "dateFormatCustom" -> updateDateFormatList()
            "theme" -> {
                // Restart activity to apply theme
                overridePendingTransition(0, 0)
                finish()
                overridePendingTransition(0, 0)
                startActivity(intent)
            }

            "updateLocationAutomatically" -> if (sharedPreferences.getBoolean(key, false)) {
                requestReadLocationPermission()
            }

            "apiKey" -> checkKey(key)
            else -> if (key.equals(
                    getString(R.string.settings_enable_notification_key),
                    ignoreCase = true
                )
            ) {
                if (sharedPreferences.getBoolean(key, false)) {
                    requestForegroundServicePermission()
                } else {
                    hideNotification()
                }
            } else if (key.equals(
                    getString(R.string.settings_notification_type_key),
                    ignoreCase = true
                )
            ) {
                setListPreferenceSummary(key)
            }
        }
    }

    private fun requestReadLocationPermission() {
        println("Calling request location permission")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Explanation not needed, since user requests this themself
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    MainActivity.MY_PERMISSIONS_ACCESS_FINE_LOCATION
                )
            }
        } else {
            privacyGuardWorkaround()
        }
    }

    private fun requestForegroundServicePermission() {
        println("Calling request foreground service permission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // this is normal permission, so no need to show explanation to user
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                MY_PERMISSIONS_FOREGROUND_SERVICE
            )
        } else {
            showNotification()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MainActivity.MY_PERMISSIONS_ACCESS_FINE_LOCATION -> {
                val permissionGranted =
                    grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                val checkBox = findPreference("updateLocationAutomatically") as CheckBoxPreference
                checkBox.isChecked = permissionGranted
                if (permissionGranted) {
                    privacyGuardWorkaround()
                }
            }

            MY_PERMISSIONS_FOREGROUND_SERVICE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showNotification()
            } else {
                val enableNotificationKey = getString(R.string.settings_enable_notification_key)
                val notificationCheckBox =
                    findPreference(enableNotificationKey) as CheckBoxPreference
                notificationCheckBox.isChecked = false
            }
        }
    }

    private fun privacyGuardWorkaround() {
        // Workaround for CM privacy guard. Register for location updates in order for it to ask us for permission
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            val dummyLocationListener: DummyLocationListener = DummyLocationListener()
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0f,
                dummyLocationListener
            )
            locationManager.removeUpdates(dummyLocationListener)
        } catch (e: SecurityException) {
            // This will most probably not happen, as we just got granted the permission
        }
    }

    private fun showNotification() {
        WeatherNotificationService.start(this)
    }

    private fun hideNotification() {
        WeatherNotificationService.stop(this)
    }

    private fun setListPreferenceSummary(preferenceKey: String?) {
        val preference = findPreference(preferenceKey) as ListPreference
        preference.summary = preference.entry
    }

    private fun setCustomDateEnabled() {
        val sp = preferenceScreen.sharedPreferences
        val customDatePref = findPreference("dateFormatCustom")
        customDatePref.isEnabled = "custom" == sp.getString("dateFormat", "")
    }

    private fun updateDateFormatList() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val res = resources
        val dateFormatPref = findPreference("dateFormat") as ListPreference
        val dateFormatsValues = res.getStringArray(R.array.dateFormatsValues)
        val dateFormatsEntries = arrayOfNulls<String>(dateFormatsValues.size)
        val customDateFormatPref = findPreference("dateFormatCustom") as EditTextPreference
        customDateFormatPref.setDefaultValue(dateFormatsValues[0])
        val sdformat = SimpleDateFormat()
        for (i in dateFormatsValues.indices) {
            val value = dateFormatsValues[i]
            if ("custom" == value) {
                var renderedCustom: String?
                renderedCustom = try {
                    sdformat.applyPattern(sp.getString("dateFormatCustom", dateFormatsValues[0]))
                    sdformat.format(SAMPLE_DATE)
                } catch (e: IllegalArgumentException) {
                    res.getString(R.string.error_dateFormat)
                }
                dateFormatsEntries[i] = String.format(
                    "%s:\n%s",
                    res.getString(R.string.setting_dateFormatCustom),
                    renderedCustom
                )
            } else {
                sdformat.applyPattern(value)
                dateFormatsEntries[i] = sdformat.format(SAMPLE_DATE)
            }
        }
        dateFormatPref.setDefaultValue(dateFormatsValues[0])
        dateFormatPref.entries = dateFormatsEntries
        setListPreferenceSummary("dateFormat")
    }

    private fun checkKey(key: String) {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (sp.getString(key, "") == "") {
            sp.edit().remove(key).apply()
        }
    }

    inner class DummyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {}
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    companion object {
        protected const val MY_PERMISSIONS_FOREGROUND_SERVICE = 2
    }
}

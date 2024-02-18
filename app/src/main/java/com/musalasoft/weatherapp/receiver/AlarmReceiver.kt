package com.musalasoft.weatherapp.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.util.Log
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.notifications.WeatherNotificationService
import com.musalasoft.weatherapp.utils.Language
import com.musalasoft.weatherapp.widgets.AbstractWidgetProvider
import com.musalasoft.weatherapp.constant.Constants
import com.musalasoft.weatherapp.R
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class AlarmReceiver : BroadcastReceiver() {
    var context: Context? = null
    override fun onReceive(context: Context, intent: Intent) {
        this.context = context
        val packageReplacedAction: Boolean
        packageReplacedAction = if (Intent.ACTION_PACKAGE_REPLACED == intent.action) {
            val packageName = intent.getStringExtra(Intent.EXTRA_UID)
            packageName != null && packageName == context.packageName
        } else {
            false
        }
        if (Intent.ACTION_BOOT_COMPLETED == intent.action || packageReplacedAction) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val interval = sp.getString("refreshInterval", "1")
            if (interval != "0") {
                setRecurringAlarm(context)
                weather
            }
            val enableNotificationKey = context.getString(R.string.settings_enable_notification_key)
            if (sp.getBoolean(enableNotificationKey, false)) {
                WeatherNotificationService.start(context)
            }
        } else if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            // Get weather if last attempt failed or if 'update location in background' is activated
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val interval = sp.getString("refreshInterval", "1")
            if (interval != "0" &&
                (sp.getBoolean("backgroundRefreshFailed", false) || isUpdateLocation)
            ) {
                weather
            }
        } else if (Intent.ACTION_LOCALE_CHANGED == intent.action) {
            WeatherNotificationService.updateNotificationChannelIfNeeded(context)
            weather
        } else {
            weather
        }
    }

    private val weather: Unit
        private get() {
            Log.d("Alarm", "Recurring alarm; requesting download service.")
            val failed: Boolean
            if (isNetworkAvailable) {
                failed = false
                if (isUpdateLocation) {
                    GetLocationAndWeatherTask().execute() // This method calls the two methods below once it has determined a location
                } else {
                    GetWeatherTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    GetLongTermWeatherTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                }
            } else {
                failed = true
            }
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putBoolean("backgroundRefreshFailed", failed)
            editor.apply()
        }
    private val isNetworkAvailable: Boolean
        private get() {
            val connectivityManager =
                context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    private val isUpdateLocation: Boolean
        private get() {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.getBoolean("updateLocationAutomatically", false)
        }

    inner class GetWeatherTask : AsyncTask<String?, String?, Void?>() {
        override fun onPreExecute() {}
        override fun doInBackground(vararg params: String?): Void? {
            try {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val language: String = Language.owmLanguage
                val apiKey = sp.getString("apiKey", context!!.resources.getString(R.string.apiKey))
                val url = URL(
                    "https://api.openweathermap.org/data/2.5/weather?id=" + URLEncoder.encode(
                        sp.getString(
                            "cityId",
                            Constants.DEFAULT_CITY_ID
                        ), "UTF-8"
                    ) + "&lang=" + language + "&appid=" + apiKey
                )
                val urlConnection = url.openConnection() as HttpURLConnection
                var connectionBufferedReader: BufferedReader? = null
                try {
                    connectionBufferedReader =
                        BufferedReader(InputStreamReader(urlConnection.inputStream))
                    if (urlConnection.responseCode == 200) {
                        val result = StringBuilder()
                        var line: String?
                        while (connectionBufferedReader.readLine().also { line = it } != null) {
                            result.append(line).append("\n")
                        }
                        val editor = sp.edit()
                        editor.putString("lastToday", result.toString())
                        editor.apply()
                        MainActivity.saveLastUpdateTime(sp)
                    } else {
                        // Connection problem
                    }
                } finally {
                    connectionBufferedReader?.close()
                }
            } catch (e: IOException) {
                // No connection
            }
            return null
        }

        override fun onPostExecute(v: Void?) {
            // Update widgets
            AbstractWidgetProvider.updateWidgets(context!!)
        }
    }

    internal inner class GetLongTermWeatherTask :
        AsyncTask<String?, String?, Void?>() {
        override fun onPreExecute() {}
        override fun doInBackground(vararg params: String?): Void? {
            try {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                val language: String = Language.owmLanguage
                val apiKey = sp.getString("apiKey", context!!.resources.getString(R.string.apiKey))
                val url = URL(
                    "https://api.openweathermap.org/data/2.5/forecast?id=" + URLEncoder.encode(
                        sp.getString(
                            "cityId",
                            Constants.DEFAULT_CITY_ID
                        ),
                        "UTF-8"
                    ) + "&lang=" + language + "&mode=json&appid=" + apiKey
                )
                val urlConnection = url.openConnection() as HttpURLConnection
                var connectionBufferedReader: BufferedReader? = null
                try {
                    connectionBufferedReader =
                        BufferedReader(InputStreamReader(urlConnection.inputStream))
                    if (urlConnection.responseCode == 200) {
                        val result = StringBuilder()
                        var line: String?
                        while (connectionBufferedReader.readLine().also { line = it } != null) {
                            result.append(line).append("\n")
                        }
                        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
                        editor.putString("lastLongterm", result.toString())
                        editor.apply()
                    } else {
                        // Connection problem
                    }
                } finally {
                    connectionBufferedReader?.close()
                }
            } catch (e: IOException) {
                // No connection
            }
            return null
        }

        override fun onPostExecute(v: Void?) {}
    }

    inner class GetLocationAndWeatherTask :
        AsyncTask<String?, String?, Void?>() {
        private val MAX_RUNNING_TIME = (30 * 1000).toDouble()
        private var locationManager: LocationManager? = null
        private var locationListener: BackgroundLocationListener? = null
        override fun onPreExecute() {
            Log.d(TAG, "Trying to determine location...")
            locationManager =
                context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationListener = BackgroundLocationListener()
            try {
                if (locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    // Only uses 'network' location, as asking the GPS every time would drain too much battery
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 0, 0f,
                        locationListener!!
                    )
                } else {
                    Log.d(
                        TAG,
                        "'Network' location is not enabled. Cancelling determining location."
                    )
                    onPostExecute(null)
                }
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "Couldn't request location updates. Probably this is an Android (>M) runtime permissions issue ",
                    e
                )
            }
        }

        override fun doInBackground(vararg params: String?): Void? {
            val startTime = System.currentTimeMillis()
            var runningTime: Long = 0
            while (locationListener!!.location == null && runningTime < MAX_RUNNING_TIME) { // Give up after 30 seconds
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Error occurred while waiting for location update", e)
                }
                runningTime = System.currentTimeMillis() - startTime
            }
            if (locationListener!!.location == null) {
                Log.d(
                    TAG,
                    String.format(
                        "Couldn't determine location in less than %s seconds",
                        MAX_RUNNING_TIME / 1000
                    )
                )
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            val location = locationListener!!.location
            if (location != null) {
                Log.d(
                    TAG,
                    String.format(
                        "Determined location: latitude %f - longitude %f",
                        location.latitude,
                        location.longitude
                    )
                )
                GetCityNameTask().execute(
                    location.latitude.toString(),
                    location.longitude.toString()
                )
            } else {
                Log.e(TAG, "Couldn't determine location. Using last known location.")
                GetWeatherTask().executeOnExecutor(THREAD_POOL_EXECUTOR)
                GetLongTermWeatherTask().executeOnExecutor(THREAD_POOL_EXECUTOR)
            }
            try {
                locationManager!!.removeUpdates(locationListener!!)
            } catch (e: SecurityException) {
                Log.e(
                    TAG,
                    "Couldn't remove location updates. Probably this is an Android (>M) runtime permissions",
                    e
                )
            }
        }

        inner class BackgroundLocationListener : LocationListener {
            var location: Location? = null
                private set

            override fun onLocationChanged(location: Location) {
                this.location = location
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}


            val TAG = "LocationListener"

        }

         val TAG = "LocationAndWTask"

    }

    inner class GetCityNameTask : AsyncTask<String?, String?, Void?>() {
        override fun doInBackground(vararg params: String?): Void? {
            val lat = params[0]
            val lon = params[1]
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val language: String = Language.owmLanguage
            val apiKey = sp.getString("apiKey", context!!.resources.getString(R.string.apiKey))
            try {
                val url =
                    URL("https://api.openweathermap.org/data/2.5/weather?q=&lat=$lat&lon=$lon&lang=$language&appid=$apiKey")
                Log.d(
                    TAG,
                    "Request: $url"
                )
                val urlConnection = url.openConnection() as HttpURLConnection
                if (urlConnection.responseCode == 200) {
                    var connectionBufferedReader: BufferedReader? = null
                    try {
                        connectionBufferedReader =
                            BufferedReader(InputStreamReader(urlConnection.inputStream))
                        val result = StringBuilder()
                        var line: String?
                        while (connectionBufferedReader.readLine().also { line = it } != null) {
                            result.append(line).append("\n")
                        }
                        Log.d(
                            TAG,
                            "JSON Result: $result"
                        )
                        val reader = JSONObject(result.toString())
                        val cityId = reader.getString("id")
                        val city = reader.getString("name")
                        var country = ""
                        val countryObj = reader.optJSONObject("sys")
                        if (countryObj != null) {
                            country = ", " + countryObj.getString("country")
                        }
                        Log.d(
                            TAG,
                            "City: $city$country"
                        )
                        val lastCity = PreferenceManager.getDefaultSharedPreferences(context)
                            .getString("city", "")
                        val currentCity = city + country
                        val editor = sp.edit()
                        editor.putString("cityId", cityId)
                        editor.putString("city", currentCity)
                        editor.putBoolean("cityChanged", currentCity != lastCity)
                        editor.commit()
                    } catch (e: JSONException) {
                        Log.e(TAG, "An error occurred while reading the JSON object", e)
                    } finally {
                        connectionBufferedReader?.close()
                    }
                } else {
                    Log.e(TAG, "Error: Response code " + urlConnection.responseCode)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection error", e)
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            GetWeatherTask().execute()
            GetLongTermWeatherTask().execute()
        }

        val TAG = "GetCityNameTask"

    }

    companion object {
        private fun intervalMillisForRecurringAlarm(intervalPref: String?): Long {
            val interval = intervalPref!!.toInt()
            return when (interval) {
                0 -> 0 // special case for cancel
                15 -> AlarmManager.INTERVAL_FIFTEEN_MINUTES
                30 -> AlarmManager.INTERVAL_HALF_HOUR
                1 -> AlarmManager.INTERVAL_HOUR
                12 -> AlarmManager.INTERVAL_HALF_DAY
                24 -> AlarmManager.INTERVAL_DAY
                else -> (interval * 3600000).toLong()
            }
        }

        fun setRecurringAlarm(context: Context) {
            val intervalPref = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("refreshInterval", "1")
            val refresh = Intent(context, AlarmReceiver::class.java)
            val recurringRefresh = PendingIntent.getBroadcast(
                context,
                0, refresh, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarms = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager
            val intervalMillis = intervalMillisForRecurringAlarm(intervalPref)
            if (intervalMillis == 0L) {
                // Cancel previous alarm
                alarms.cancel(recurringRefresh)
            } else {
                alarms.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + intervalMillis,
                    intervalMillis,
                    recurringRefresh
                )
            }
        }
    }
}

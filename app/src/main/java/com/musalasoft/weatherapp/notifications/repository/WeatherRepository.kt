package com.musalasoft.weatherapp.notifications.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.ImmutableWeather
import com.musalasoft.weatherapp.models.WeatherPresentation
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatterType
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

/**
 * Observe change in the Shared Preferences and update WeatherPresentation when weather info or
 * settings how to show weather is changed.
 *
 * Implementation Note: Observer pattern is preferable than use of startService with data in
 * Intent when some class updates data because Observer pattern grants us one source of truth.
 */
class WeatherRepository(context: Context, private val executor: Executor) {
    private var notificationTypeKey: String? = null
    private var notificationTypeDefaultKey: String? = null
    private var notificationTypeSimpleKey: String? = null
    private var notificationTypeDefault: String? = null
    private var showTemperatureInStatusBarKey: String? = null
    private var prefs: SharedPreferences?
    private val listeners: MutableSet<WeakReference<RepositoryListener>> = HashSet()
    private var onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener? = null
    private val weatherPresentation: AtomicReference<WeatherPresentation?> =
        AtomicReference<WeatherPresentation?>()

    init {
        prepareSettingsConstants(context)
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    val weather: WeatherPresentation
        get() {
            var result: WeatherPresentation = weatherPresentation.get()!!
            if (listeners.isEmpty() || result == null) {
                result = readValuesFromStorage()
            }
            return result
        }

    /**
     * Start to observe weather data and settings. Current data will be emitted immediately.
     * <br></br>
     * Repository doesn't hold strong reference on the listener so you should keep it while it is
     * needed by yourself.
     * <br></br>
     * NOTE: do NOT use this method after call to [.clear]
     * @param repositoryListener listener to emit data.
     * @throws IllegalStateException if repository already cleared by call of [.clear]
     */
    @Throws(IllegalStateException::class)
    fun observeWeather(repositoryListener: RepositoryListener) {
        checkNotNull(prefs) { "DO NOT call this method after clear." }
        synchronized(listeners) {
            listeners.add(
                WeakReference(
                    repositoryListener
                )
            )
            if (onSharedPreferenceChangeListener == null) {
                onSharedPreferenceChangeListener =
                    OnChangeListener(
                        weatherPresentation
                    )
                prefs!!.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)
            }
            if (listeners.size == 1) {
                weatherPresentation.set(readValuesFromStorage())
            }
            repositoryListener.onChange(weatherPresentation.get()!!)
        }
    }

    /**
     * Clear resources.
     *
     * NOTE: do NOT use [.observeWeather] after this call.
     */
    fun clear() {
        synchronized(listeners) { listeners.clear() }
        val prefs = prefs
        if (prefs != null) {
            val listener = onSharedPreferenceChangeListener
            if (listener != null) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
                onSharedPreferenceChangeListener = null
            }
            this.prefs = null
        }
    }

    /**
     * Initialize values from Shared Preferences to show weather info into notification.
     */
    private fun readValuesFromStorage(): WeatherPresentation {
        val type: WeatherFormatterType = readNotificationType(prefs!!)
        val json = prefs!!.getString("lastToday", "{}")
        val lastUpdate = prefs!!.getLong("lastUpdate", -1L)
        val weather: ImmutableWeather = ImmutableWeather.fromJson(json!!, lastUpdate)
        return WeatherPresentation(
            prefs!!.getBoolean(
                "temperatureInteger",
                WeatherPresentation.DEFAULT_DO_ROUND_TEMPERATURE
            ),
            prefs!!.getString("unit", WeatherPresentation.DEFAULT_TEMPERATURE_UNITS)!!,
            prefs!!.getString("speedUnit", WeatherPresentation.DEFAULT_WIND_SPEED_UNITS)!!,
            prefs!!.getString(
                "windDirectionFormat",
                WeatherPresentation.DEFAULT_WIND_DIRECTION_FORMAT
            )!!,
            prefs!!.getString("pressureUnit", WeatherPresentation.DEFAULT_PRESSURE_UNITS)!!,
            prefs!!.getBoolean(
                showTemperatureInStatusBarKey,
                WeatherPresentation.DEFAULT_SHOW_TEMPERATURE_IN_STATUS_BAR
            ),
            weather, type
        )
    }

    /** Retrieve notification type from preferences.  */
    private fun readNotificationType(prefs: SharedPreferences): WeatherFormatterType {
        val result: WeatherFormatterType
        val typePref = prefs.getString(notificationTypeKey, notificationTypeDefault)
        result = if (typePref != null && typePref.equals(
                notificationTypeDefaultKey,
                ignoreCase = true
            )
        ) {
            WeatherFormatterType.NOTIFICATION_DEFAULT
        } else if (typePref != null && typePref.equals(
                notificationTypeSimpleKey,
                ignoreCase = true
            )
        ) {
            WeatherFormatterType.NOTIFICATION_SIMPLE
        } else {
            if (notificationTypeDefault == null || notificationTypeDefault.equals(
                    notificationTypeDefaultKey,
                    ignoreCase = true
                )
            ) {
                WeatherFormatterType.NOTIFICATION_DEFAULT
            } else {
                WeatherFormatterType.NOTIFICATION_SIMPLE
            }
        }
        return result
    }

    private fun prepareSettingsConstants(context: Context) {
        notificationTypeKey = context.getString(R.string.settings_notification_type_key)
        notificationTypeDefaultKey =
            context.getString(R.string.settings_notification_type_key_default)
        notificationTypeSimpleKey =
            context.getString(R.string.settings_notification_type_key_simple)
        notificationTypeDefault =
            context.getString(R.string.settings_notification_type_default_value)
        showTemperatureInStatusBarKey =
            context.getString(R.string.settings_show_temperature_in_status_bar_key)
    }

    /** Callback method to get updated weather data and settings.  */
    interface RepositoryListener {
        /**
         * This method will be invoked immediately at beginning of observation and on every data or
         * settings update.
         * @param newData weather data and settings.
         */
        fun onChange(newData: WeatherPresentation)
    }

    inner class OnChangeListener(weatherPresentation: AtomicReference<WeatherPresentation?>) :
        OnSharedPreferenceChangeListener {
        private val weatherPresentation: AtomicReference<WeatherPresentation?>

        init {
            this.weatherPresentation = weatherPresentation
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            executor.execute(UpdateDataFromStorage(weatherPresentation, sharedPreferences, key!!))
        }

        inner class UpdateDataFromStorage(
            weatherPresentation: AtomicReference<WeatherPresentation?>,
            sharedPreferences: SharedPreferences, key: String
        ) : Runnable {
            private val weatherPresentation: AtomicReference<WeatherPresentation?>
            private val sharedPreferences: SharedPreferences?
            private val key: String?

            init {
                this.weatherPresentation = weatherPresentation
                this.sharedPreferences = sharedPreferences
                this.key = key
            }

            override fun run() {
                if (sharedPreferences == null || key == null) return
                val weatherPresentation: WeatherPresentation? = this.weatherPresentation.get()
                var result: WeatherPresentation? = null
                when (key) {
                    "lastUpdate", "lastToday" -> {
                        val json = sharedPreferences.getString("lastToday", "")
                        if (json != null && !json.isEmpty()) {
                            val lastUpdate = sharedPreferences.getLong("lastUpdate", -1L)
                            val weather: ImmutableWeather =
                                ImmutableWeather.fromJson(json, lastUpdate)
                            result = weatherPresentation!!.copy(weather)
                        }
                    }

                    "temperatureInteger" -> {
                        val roundTemperature = sharedPreferences.getBoolean(
                            key,
                            WeatherPresentation.DEFAULT_DO_ROUND_TEMPERATURE
                        )
                        result = weatherPresentation!!.copy(roundTemperature)
                    }

                    "unit" -> {
                        val temperatureUnits = sharedPreferences.getString(
                            key,
                            WeatherPresentation.DEFAULT_TEMPERATURE_UNITS
                        )
                        if (temperatureUnits != null) {
                            result = weatherPresentation!!.copyTemperatureUnits(temperatureUnits)
                        }
                    }

                    "speedUnit" -> {
                        val windSpeedUnits = sharedPreferences.getString(
                            key,
                            WeatherPresentation.DEFAULT_WIND_SPEED_UNITS
                        )
                        if (windSpeedUnits != null) {
                            result = weatherPresentation!!.copyWindSpeedUnits(windSpeedUnits)
                        }
                    }

                    "windDirectionFormat" -> {
                        val windDirectionFormat = sharedPreferences.getString(
                            key,
                            WeatherPresentation.DEFAULT_WIND_DIRECTION_FORMAT
                        )
                        if (windDirectionFormat != null) {
                            result =
                                weatherPresentation!!.copyWindDirectionFormat(windDirectionFormat)
                        }
                    }

                    "pressureUnit" -> {
                        val pressureUnits = sharedPreferences.getString(
                            key,
                            WeatherPresentation.DEFAULT_PRESSURE_UNITS
                        )
                        if (pressureUnits != null) {
                            result = weatherPresentation!!.copyPressureUnits(pressureUnits)
                        }
                    }

                    else -> if (key.equals(notificationTypeKey, ignoreCase = true)) {
                        result = weatherPresentation!!.copy(readNotificationType(sharedPreferences))
                    } else if (key.equals(showTemperatureInStatusBarKey, ignoreCase = true)) {
                        val showTemperatureInStatusBar = sharedPreferences.getBoolean(
                            showTemperatureInStatusBarKey,
                            WeatherPresentation.DEFAULT_SHOW_TEMPERATURE_IN_STATUS_BAR
                        )
                        result = weatherPresentation!!.copyShowTemperatureInStatusBar(
                            showTemperatureInStatusBar
                        )
                    }
                }
                if (result != null && this.weatherPresentation.compareAndSet(
                        weatherPresentation,
                        result
                    )
                ) {
                    Handler(Looper.getMainLooper()).post(PostData(result))
                }
            }
        }

        inner class PostData(weatherPresentation: WeatherPresentation?) :
            Runnable {
            private val weatherPresentation: WeatherPresentation?

            init {
                this.weatherPresentation = weatherPresentation
            }

            override fun run() {
                if (weatherPresentation != null) {
                    var listener: RepositoryListener
                    val iter = listeners.iterator()
                    while (iter.hasNext()) {
                        listener = iter.next().get()!!
                        if (listener != null) {
                            listener.onChange(weatherPresentation)
                        } else {
                            iter.remove()
                        }
                    }
                }
                if (listeners.isEmpty()) {
                    if (prefs != null) {
                        prefs!!.unregisterOnSharedPreferenceChangeListener(this@OnChangeListener)
                        onSharedPreferenceChangeListener = null
                    }
                }
            }
        }
    }
}
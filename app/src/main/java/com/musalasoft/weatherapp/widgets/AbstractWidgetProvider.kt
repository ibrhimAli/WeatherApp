package com.musalasoft.weatherapp.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.util.Log
import android.widget.RemoteViews
import com.musalasoft.weatherapp.BuildConfig
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.utils.Formatting
import com.musalasoft.weatherapp.utils.TimeUtils
import com.musalasoft.weatherapp.utils.UnitConvertor
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter
import com.musalasoft.weatherapp.weatherapi.WeatherStorage
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

abstract class AbstractWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_UPDATE_TIME == intent.action) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context.packageName, javaClass.name)
            val ids = appWidgetManager.getAppWidgetIds(provider)
            onUpdate(context, appWidgetManager, ids)
        } else {
            super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(this.javaClass.simpleName, "Disable updates for this widget")
        cancelUpdate(context)
    }

    protected fun getWeatherIcon(weather: Weather, context: Context?): Bitmap {
        val formatting = Formatting(context!!)
        val weatherIcon: String = formatting.getWeatherIcon(
            weather.weatherId,
            TimeUtils.isDayTime(weather, Calendar.getInstance())
        )
        return WeatherFormatter.getWeatherIconAsBitmap(context, weatherIcon, Color.WHITE)
    }

    protected fun getTodayWeather(context: Context?): Weather? {
        val weatherStorage = WeatherStorage(context)
        return weatherStorage.lastToday as Weather?
    }

    protected fun openMainActivity(context: Context?, remoteViews: RemoteViews) {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent,
                0 or PendingIntent.FLAG_IMMUTABLE)
            remoteViews.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
            pendingIntent.send()
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
    }

    protected fun localize(
        sp: SharedPreferences?, context: Context?, preferenceKey: String?,
        defaultValueKey: String?
    ): String {
        MainActivity.initMappings()
        return MainActivity.localize(sp!!, context!!, preferenceKey!!, defaultValueKey)!!
    }

    protected fun setTheme(context: Context?, remoteViews: RemoteViews) {
        if (PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("transparentWidget", false)
        ) {
            remoteViews.setInt(
                R.id.widgetRoot,
                "setBackgroundResource",
                R.drawable.widget_card_transparent
            )
            return
        }
        val theme =
            PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "fresh")
        when (theme) {
            "dark", "classicdark" -> remoteViews.setInt(
                R.id.widgetRoot,
                "setBackgroundResource",
                R.drawable.widget_card_dark
            )

            "black", "classicblack" -> remoteViews.setInt(
                R.id.widgetRoot,
                "setBackgroundResource",
                R.drawable.widget_card_black
            )

            "classic" -> remoteViews.setInt(
                R.id.widgetRoot,
                "setBackgroundResource",
                R.drawable.widget_card_classic
            )

            else -> remoteViews.setInt(
                R.id.widgetRoot,
                "setBackgroundResource",
                R.drawable.widget_card
            )
        }
    }

    protected fun scheduleNextUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = Date().time
        val nextUpdate = now + DURATION_MINUTE - now % DURATION_MINUTE
        if (BuildConfig.DEBUG) {
            Log.v(
                this.javaClass.simpleName, "Next widget update: " +
                        DateFormat.getTimeFormat(context).format(Date(nextUpdate))
            )
        }
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC, nextUpdate, getTimeIntent(context))
        } else {
            alarmManager[AlarmManager.RTC, nextUpdate] = getTimeIntent(context)
        }
    }

    protected fun cancelUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(getTimeIntent(context))
    }

    protected fun getTimeIntent(context: Context?): PendingIntent {
        val intent = Intent(context, this.javaClass)
        intent.setAction(ACTION_UPDATE_TIME)
        return PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    protected fun getFormattedTemperature(
        weather: Weather,
        context: Context?,
        sp: SharedPreferences
    ): String {
        var temperature: Float =
            UnitConvertor.convertTemperature(weather.temperature.toFloat(), sp)
        if (sp.getBoolean("temperatureInteger", false)) {
            temperature = Math.round(temperature).toFloat()
        }
        return DecimalFormat("#.#").format(temperature.toDouble()) + localize(
            sp,
            context,
            "unit",
            "C"
        )
    }

    protected fun getFormattedPressure(
        weather: Weather,
        context: Context?,
        sp: SharedPreferences?
    ): String {
        val pressure: Double = UnitConvertor.convertPressure(weather.pressure, sp!!).toDouble()
        return DecimalFormat("0.0").format(pressure) + " " + localize(
            sp,
            context,
            "pressureUnit",
            "hPa"
        )
    }

    protected fun getFormattedWind(
        weather: Weather,
        context: Context?,
        sp: SharedPreferences?
    ): String {
        val wind: Double = UnitConvertor.convertWind(weather.wind, sp!!)
        return (DecimalFormat("0.0").format(wind) + " " + localize(sp, context, "speedUnit", "m/s")
                + if (weather.isWindDirectionAvailable) " " + MainActivity.getWindDirectionString(
            sp,
            context,
            weather
        ) else "")
    }

    companion object {
        protected val DURATION_MINUTE = TimeUnit.SECONDS.toMillis(30)
        protected const val ACTION_UPDATE_TIME = "com.musalasoft.weatherapp.UPDATE_TIME"
        fun updateWidgets(context: Context) {
            updateWidgets(context, ExtensiveWidgetProvider::class.java)
            updateWidgets(context, TimeWidgetProvider::class.java)
            updateWidgets(context, SimpleWidgetProvider::class.java)
            updateWidgets(context, ClassicTimeWidgetProvider::class.java)
        }

        private fun updateWidgets(context: Context, widgetClass: Class<*>) {
            val intent = Intent(context.applicationContext, widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            val ids = AppWidgetManager.getInstance(context.applicationContext)
                .getAppWidgetIds(ComponentName(context.applicationContext, widgetClass))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.applicationContext.sendBroadcast(intent)
        }
    }
}

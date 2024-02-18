package com.musalasoft.weatherapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.models.Weather

class ExtensiveWidgetProvider : AbstractWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.extensive_widget
            )
            setTheme(context, remoteViews)
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            remoteViews.setOnClickPendingIntent(R.id.widgetButtonRefresh, pendingIntent)
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val widgetWeather: Weather? = this.getTodayWeather(context)
            if (widgetWeather == null) {
                this.openMainActivity(context, remoteViews)
                return
            }
            val timeFormat = DateFormat.getTimeFormat(context)
            val lastUpdated = context.getString(
                R.string.last_update_widget,
                MainActivity.formatTimeWithDayIfNotToday(context, widgetWeather.lastUpdated)
            )
            remoteViews.setTextViewText(
                R.id.widgetCity,
                widgetWeather.city + ", " + widgetWeather.country
            )
            remoteViews.setTextViewText(
                R.id.widgetTemperature,
                this.getFormattedTemperature(widgetWeather, context, sp)
            )
            remoteViews.setTextViewText(R.id.widgetDescription, widgetWeather.description)
            remoteViews.setTextViewText(
                R.id.widgetWind,
                context.getString(R.string.wind) + ": " + this.getFormattedWind(
                    widgetWeather,
                    context,
                    sp
                )
            )
            remoteViews.setTextViewText(
                R.id.widgetPressure,
                context.getString(R.string.pressure) + ": " + this.getFormattedPressure(
                    widgetWeather,
                    context,
                    sp
                )
            )
            remoteViews.setTextViewText(
                R.id.widgetHumidity,
                (context.getString(R.string.humidity) + ": " + widgetWeather.humidity) + " %"
            )
            remoteViews.setTextViewText(
                R.id.widgetSunrise,
                context.getString(R.string.sunrise) + ": " + timeFormat.format(widgetWeather.sunrise!!)
            ) //
            remoteViews.setTextViewText(
                R.id.widgetSunset,
                context.getString(R.string.sunset) + ": " + timeFormat.format(widgetWeather.sunset!!)
            )
            remoteViews.setTextViewText(R.id.widgetLastUpdate, lastUpdated)
            remoteViews.setImageViewBitmap(R.id.widgetIcon, getWeatherIcon(widgetWeather, context))
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
        scheduleNextUpdate(context)
    }
}

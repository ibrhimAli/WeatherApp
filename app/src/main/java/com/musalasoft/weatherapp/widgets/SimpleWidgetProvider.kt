package com.musalasoft.weatherapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.RemoteViews
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.Weather

class SimpleWidgetProvider : AbstractWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.simple_widget
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
            remoteViews.setTextViewText(
                R.id.widgetCity,
                widgetWeather.city + ", " + widgetWeather.country
            )
            remoteViews.setTextViewText(
                R.id.widgetTemperature,
                this.getFormattedTemperature(widgetWeather, context, sp)
            )
            remoteViews.setTextViewText(R.id.widgetDescription, widgetWeather.description)
            remoteViews.setImageViewBitmap(R.id.widgetIcon, getWeatherIcon(widgetWeather, context))
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
        scheduleNextUpdate(context)
    }
}

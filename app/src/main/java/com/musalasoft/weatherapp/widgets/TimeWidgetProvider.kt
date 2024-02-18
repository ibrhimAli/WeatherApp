package com.musalasoft.weatherapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.widget.RemoteViews
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.Weather
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class TimeWidgetProvider : AbstractWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.time_widget
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
            val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
            val defaultDateFormat = context.resources.getStringArray(R.array.dateFormatsValues)[0]
            var simpleDateFormat = sp.getString("dateFormat", defaultDateFormat)
            if ("custom" == simpleDateFormat) {
                simpleDateFormat = sp.getString("dateFormatCustom", defaultDateFormat)
            }
            var dateString: String
            try {
                simpleDateFormat =
                    simpleDateFormat!!.substring(0, simpleDateFormat.indexOf("-") - 1)
                dateString = try {
                    val resultFormat = SimpleDateFormat(simpleDateFormat)
                    resultFormat.format(Date())
                } catch (e: IllegalArgumentException) {
                    context.resources.getString(R.string.error_dateFormat)
                }
            } catch (e: StringIndexOutOfBoundsException) {
                val dateFormat = DateFormat.getDateInstance(DateFormat.LONG)
                dateString = dateFormat.format(Date())
            }
            remoteViews.setTextViewText(R.id.time, timeFormat.format(Date()))
            remoteViews.setTextViewText(R.id.date, dateString)
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
            if (dateString.length > 19) remoteViews.setViewPadding(R.id.widgetIcon, 40, 0, 0, 0)
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
        scheduleNextUpdate(context)
    }
}

package com.musalasoft.weatherapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.text.format.DateFormat
import android.util.Log
import android.widget.RemoteViews
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.Weather
import java.text.SimpleDateFormat
import java.util.Date

class ClassicTimeWidgetProvider : AbstractWidgetProvider() {
    //private static final long DURATION_MINUTE = TimeUnit.SECONDS.toMillis(30);
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(
                context.packageName,
                R.layout.time_widget_classic
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
            val defaultDateFormat = context.resources.getStringArray(R.array.dateFormatsValues)[0]
            var dateFormat = sp.getString("dateFormat", defaultDateFormat)
            dateFormat = dateFormat!!.substring(0, dateFormat.indexOf("-") - 1)
            if ("custom" == dateFormat) {
                dateFormat = sp.getString("dateFormatCustom", defaultDateFormat)
            }
            var dateString: String
            dateString = try {
                val resultFormat = SimpleDateFormat(dateFormat)
                resultFormat.format(Date())
            } catch (e: IllegalArgumentException) {
                context.resources.getString(R.string.error_dateFormat)
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
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
        scheduleNextUpdate(context)
    }

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
        Log.d(TAG, "Disable time widget updates")
        cancelUpdate(context)
    } /*
    private static void scheduleNextUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = new Date().getTime();
        long nextUpdate = now + DURATION_MINUTE - now % DURATION_MINUTE;
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "Next widget update: " +
                    android.text.format.DateFormat.getTimeFormat(context).format(new Date(nextUpdate)));
        }
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC, nextUpdate, getTimeIntent(context));
        } else {
            alarmManager.set(AlarmManager.RTC, nextUpdate, getTimeIntent(context));
        }
    }

    private static void cancelUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getTimeIntent(context));
    }

    private static PendingIntent getTimeIntent(Context context) {
        Intent intent = new Intent(context, TimeWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_TIME);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }
    */

    companion object {
        private const val TAG = "TimeWidgetProvider"
        private const val ACTION_UPDATE_TIME = "com.musalasoft.weatherapp.UPDATE_TIME"
    }
}

package com.musalasoft.weatherapp.notifications.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.WeatherPresentation
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter
import com.musalasoft.weatherapp.utils.formatters.WeatherSimpleNotificationFormatter

class SimpleNotificationContentUpdater(formatter: WeatherFormatter) : NotificationContentUpdater() {
    private val formatter: WeatherFormatter

    init {
        this.formatter = formatter
    }

    override val isLayoutCustom: Boolean
        get() = true

    @Throws(NullPointerException::class)
    override fun prepareRemoteView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.notification_simple)
    }

    @Throws(NullPointerException::class)
    override fun updateNotification(
        weatherPresentation: WeatherPresentation,
        notification: NotificationCompat.Builder,
        notificationLayout: RemoteViews,
        context: Context
    ) {
        if (weatherPresentation == null) throw NullPointerException("weatherPresentation is null")
        if (notification == null) throw NullPointerException("notification is null")
        if (notificationLayout == null) throw NullPointerException("notificationLayout is null")
        if (context == null) throw NullPointerException("context is null")
        super.updateNotification(weatherPresentation, notification, notificationLayout, context)
        notification // Too much information for decorated view. Only two strings fit.
            /*.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)*/
            .setContent(notificationLayout)
            .setCustomBigContentView(notificationLayout)
        if (formatter.isEnoughValidData(weatherPresentation.getWeather())) {
            setTemperatureAndDescription(notificationLayout, weatherPresentation)
            notificationLayout.setViewVisibility(R.id.icon, View.VISIBLE)
            val weatherIcon: Bitmap =
                formatter.getWeatherIconAsBitmap(weatherPresentation.getWeather(), context)
            notificationLayout.setImageViewBitmap(R.id.icon, weatherIcon)
            val wind: String = formatter.getWind(
                weatherPresentation.getWeather(),
                weatherPresentation.windSpeedUnits,
                weatherPresentation.windDirectionFormat, context
            )
            notificationLayout.setTextViewText(R.id.wind, wind)
            val pressure: String = formatter.getPressure(
                weatherPresentation.getWeather(),
                weatherPresentation.pressureUnits, context
            )
            notificationLayout.setTextViewText(R.id.pressure, pressure)
            val humidity: String = formatter.getHumidity(weatherPresentation.getWeather(), context)
            notificationLayout.setTextViewText(R.id.humidity, humidity)
        } else {
            if (formatter is WeatherSimpleNotificationFormatter
                && formatter
                    .isEnoughValidMainData(weatherPresentation.getWeather())
            ) {
                setTemperatureAndDescription(notificationLayout, weatherPresentation)
            } else {
                notificationLayout.setTextViewText(R.id.temperature, "")
                notificationLayout.setTextViewText(R.id.description, "")
            }
            notificationLayout.setViewVisibility(R.id.icon, View.GONE)
            notificationLayout.setTextViewText(R.id.wind, "")
            notificationLayout.setTextViewText(R.id.pressure, context.getString(R.string.no_data))
            notificationLayout.setTextViewText(R.id.humidity, "")
        }
    }

    private fun setTemperatureAndDescription(
        notificationLayout: RemoteViews,
        weatherPresentation: WeatherPresentation
    ) {
        val temperature: String = formatter.getTemperature(
            weatherPresentation.getWeather(),
            weatherPresentation.temperatureUnits,
            weatherPresentation.isRoundedTemperature
        )
        notificationLayout.setTextViewText(R.id.temperature, temperature)
        val description: String = formatter.getDescription(weatherPresentation.getWeather())
        notificationLayout.setTextViewText(R.id.description, description)
    }
}
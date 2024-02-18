package com.musalasoft.weatherapp.notifications.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.WeatherPresentation
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter

/**
 * Update notification content for default notification view.
 */
class DefaultNotificationContentUpdater(formatter: WeatherFormatter) :
    NotificationContentUpdater() {
    private val formatter: WeatherFormatter

    init {
        this.formatter = formatter
    }

    override val isLayoutCustom: Boolean
        get() = false

    @Throws(NullPointerException::class)
    override fun updateNotification(
        weatherPresentation: WeatherPresentation,
        notification: NotificationCompat.Builder,
        context: Context
    ) {
        if (weatherPresentation == null) throw NullPointerException("weatherPresentation is null")
        if (notification == null) throw NullPointerException("notification is null")
        if (context == null) throw NullPointerException("context is null")
        super.updateNotification(weatherPresentation, notification, context)
        notification
            .setCustomContentView(null)
            .setContent(null)
            .setCustomBigContentView(null)
            .setColorized(false)
            .setColor(NotificationCompat.COLOR_DEFAULT)
        if (formatter.isEnoughValidData(weatherPresentation.getWeather())) {
            val temperature: String = formatter.getTemperature(
                weatherPresentation.getWeather(),
                weatherPresentation.temperatureUnits,
                weatherPresentation.isRoundedTemperature
            )
            notification
                .setContentTitle(temperature)
                .setContentText(formatter.getDescription(weatherPresentation.getWeather()))
                .setLargeIcon(
                    formatter.getWeatherIconAsBitmap(
                        weatherPresentation.getWeather(),
                        context
                    )
                )
        } else {
            notification.setContentTitle(context.getString(R.string.no_data))
                .setContentText(null)
                .setLargeIcon(null as Bitmap?)
        }
    }
}
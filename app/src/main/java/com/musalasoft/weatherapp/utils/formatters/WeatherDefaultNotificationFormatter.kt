package com.musalasoft.weatherapp.utils.formatters

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.ImmutableWeather
import com.musalasoft.weatherapp.utils.TimeUtils.isDayTime

/**
 * Formatter for notification in default Android style with only title, short text and icon.
 */
class WeatherDefaultNotificationFormatter : WeatherFormatter() {
    /**
     * {@inheritDoc}
     * @throws NullPointerException if `weather` is null
     */
    @Throws(NullPointerException::class)
    override fun isEnoughValidData(weather: ImmutableWeather): Boolean {
        return weather.temperature !== ImmutableWeather.EMPTY.temperature && !weather.description
            .equals(ImmutableWeather.EMPTY.description) && weather.weatherIcon !== ImmutableWeather.EMPTY.weatherIcon
    }

    /**
     * Returns temperature with units.
     * @param weather weather info
     * @param temperatureUnit temperature units
     * @param roundedTemperature if `true` round temperature and show as integer
     * @return temperature with units
     * @throws NullPointerException if any of parameters is null
     */
    @Throws(NullPointerException::class)
    override fun getTemperature(
        weather: ImmutableWeather,
        temperatureUnit: String,
        roundedTemperature: Boolean
    ): String {
        return TemperatureFormatter.getTemperature(weather, temperatureUnit, roundedTemperature)
    }

    /**
     * Returns weather description with first uppercase letter.
     * @param weather weather info
     * @return weather description with first uppercase letter
     * @throws NullPointerException if `weather` is null
     */
    @Throws(NullPointerException::class)
    override fun getDescription(weather: ImmutableWeather): String {
        return DescriptionFormatter.getDescription(weather)
    }

    /**
     * Returns weather icon as [Bitmap].
     * @param weather weather info
     * @param context android context
     * @return weather icon as [Bitmap]
     */
    override fun getWeatherIconAsBitmap(
        weather: ImmutableWeather,
        context: Context
    ): Bitmap {
        val icon: String =
            getWeatherIconAsText(weather.weatherIcon, isDayTime(weather), context)
        val color = ContextCompat.getColor(context, R.color.notification_icon_color)
        return getWeatherIconAsBitmap(context, icon, color)
    }
}

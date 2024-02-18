package com.musalasoft.weatherapp.utils.formatters

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.ImmutableWeather
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.utils.TimeUtils.isDayTime
import com.musalasoft.weatherapp.utils.UnitConvertor
import com.musalasoft.weatherapp.utils.localizers.PressureUnitsLocalizer
import com.musalasoft.weatherapp.utils.localizers.WindDirectionLocalizer
import com.musalasoft.weatherapp.utils.localizers.WindSpeedUnitsLocalizer
import java.text.DecimalFormat
import java.util.Locale

class WeatherSimpleNotificationFormatter : WeatherFormatter() {
    /**
     * {@inheritDoc}
     * @throws NullPointerException if `weather` is null
     */
    @Throws(NullPointerException::class)
    override fun isEnoughValidData(weather: ImmutableWeather): Boolean {
        return weather.temperature !== ImmutableWeather.EMPTY.temperature && !weather.description
            .equals(ImmutableWeather.EMPTY.description) && weather.weatherIcon !== ImmutableWeather.EMPTY.weatherIcon && weather.windSpeed !== ImmutableWeather.EMPTY.windSpeed && weather.getWindDirection() !== ImmutableWeather.EMPTY.getWindDirection() && weather.pressure !== ImmutableWeather.EMPTY.pressure && weather.humidity !== ImmutableWeather.EMPTY.humidity
    }

    /**
     * Check is there enough data to show main part (e.g. is there temperature).
     * @param weather weather information
     * @return `true` if there is valid temperature and `false` otherwise
     * @throws NullPointerException if `weather` is null
     */
    @Throws(NullPointerException::class)
    fun isEnoughValidMainData(weather: ImmutableWeather): Boolean {
        return weather.temperature !== ImmutableWeather.EMPTY.temperature
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
        weather: ImmutableWeather, temperatureUnit: String,
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
     * {@inheritDoc}
     * @throws NullPointerException if any of parameters is null
     */
    @Throws(NullPointerException::class)
    override fun getWind(
        weather: ImmutableWeather, units: String,
        directionFormat: String, context: Context
    ): String {
        val builder = StringBuilder()
        if (weather.windSpeed !== ImmutableWeather.EMPTY.windSpeed) {
            builder
                .append(context.getString(R.string.wind))
                .append(": ")
            try {
                val windSpeed: Double = weather.getWindSpeed(units)
                if (units == "bft") builder.append(
                    UnitConvertor.getBeaufortName(
                        windSpeed.toInt(),
                        context
                    )
                ) else {
                    builder.append(DecimalFormat("0.0").format(windSpeed))
                    builder
                        .append(' ')
                        .append(WindSpeedUnitsLocalizer.localizeWindSpeedUnits(units, context))
                }
                val windDirection: Weather.WindDirection = weather.getWindDirection()!!
                try {
                    val localizedWindDirection: String =
                        WindDirectionLocalizer.localizeWindDirection(
                            windDirection, directionFormat, context
                        )
                    if (!localizedWindDirection.isEmpty()) {
                        builder
                            .append(' ')
                            .append(localizedWindDirection)
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                builder.delete(0, builder.length)
            }
        }
        return builder.toString()
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if any of parameters is null
     */
    @Throws(NullPointerException::class)
    override fun getPressure(
        weather: ImmutableWeather, units: String,
        context: Context
    ): String {
        val builder = StringBuilder()
        if (weather.pressure !== ImmutableWeather.EMPTY.pressure) {
            builder
                .append(context.getString(R.string.pressure))
                .append(DecimalFormat(": 0.0 ").format(weather.getPressure(units)))
            try {
                builder
                    .append(PressureUnitsLocalizer.localizePressureUnits(units, context))
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                builder.delete(0, builder.length)
            }
        }
        return builder.toString()
    }

    /**
     * {@inheritDoc}
     * @throws NullPointerException if any of parameters is null
     */
    @Throws(NullPointerException::class)
    override fun getHumidity(weather: ImmutableWeather, context: Context): String {
        val result: String
        result = if (weather.humidity !== ImmutableWeather.EMPTY.humidity) {
            java.lang.String.format(
                Locale.getDefault(), "%s: %d %%",
                context.getString(R.string.humidity),
                weather.humidity
            )
        } else {
            ""
        }
        return result
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

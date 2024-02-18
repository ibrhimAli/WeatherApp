package com.musalasoft.weatherapp.utils.formatters

import com.musalasoft.weatherapp.models.ImmutableWeather
import java.text.DecimalFormat

/**
 * Formatter for temperature.
 * <br></br>
 * Format temperature with units like: 15.3K, 12°C
 */
// TODO rid off static and use DI
object TemperatureFormatter {
    /**
     * Returns temperature with units.
     * @param weather weather info
     * @param temperatureUnit temperature units
     * @param roundedTemperature if `true` round temperature and show as integer
     * @return temperature with units
     * @throws NullPointerException if any of parameters is null
     */
    @Throws(NullPointerException::class)
    fun getTemperature(
        weather: ImmutableWeather,
        temperatureUnit: String,
        roundedTemperature: Boolean
    ): String {
        val temperature: String = if (roundedTemperature) java.lang.String.valueOf(
            weather.getRoundedTemperature(temperatureUnit)
        ) else DecimalFormat("0.#").format(weather.getTemperature(temperatureUnit))
        return temperature + temperatureUnit
    }
}

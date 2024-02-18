package com.musalasoft.weatherapp.utils.formatters

import com.musalasoft.weatherapp.models.ImmutableWeather
import java.util.Locale

/**
 * Formatter for weather description.
 */
// TODO rid off static and use DI
object DescriptionFormatter {
    /**
     * Returns weather description with first uppercase letter.
     * @param weather weather info
     * @return weather description with first uppercase letter
     * @throws NullPointerException if `weather` is null
     */
    @Throws(NullPointerException::class)
    fun getDescription(weather: ImmutableWeather): String {
        val description: String = weather.description
        return if (description.isEmpty()) description else description.substring(0, 1).uppercase(
            Locale.getDefault()
        ) + description.substring(1)
    }
}

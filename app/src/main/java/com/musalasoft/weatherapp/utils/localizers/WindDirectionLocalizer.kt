package com.musalasoft.weatherapp.utils.localizers

import android.content.Context
import com.musalasoft.weatherapp.models.Weather

/**
 * Class to apply specified format and localize (translate) wind direction to current locale.
 */
// TODO replace "singleton" with DI
object WindDirectionLocalizer {
    /**
     * Returns wind direction in specified format and localize it if needed.
     * @param direction wind direction
     * @param format resulted format
     * @param context android context
     * @return formatted and localized wind direction
     * @throws NullPointerException if any of parameters is null
     * @throws IllegalArgumentException if `format` have value other than "abbr", "arrow" or "none"
     */
    // TODO replace String with enum
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun localizeWindDirection(
        direction: Weather.WindDirection,
        format: String,
        context: Context
    ): String {
        if (direction == null) throw NullPointerException("direction should not be null")
        if (format == null) throw NullPointerException("format should not be null")
        if (context == null) throw NullPointerException("context should not be null")
        val result: String
        result = when (format) {
            "abbr" -> direction.getLocalizedString(context)
            "arrow" -> direction.getArrow(context)
            "none" -> ""
            else -> throw IllegalArgumentException("Unknown format: \"$format\"")
        }
        return result
    }
}

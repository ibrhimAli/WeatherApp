package com.musalasoft.weatherapp.utils.formatters

/**
 * Factory for creating formatters by type.
 */
object WeatherFormatterFactory {
    /**
     * Create formatter for specified type.
     * @param type type of formatter
     * @return new formatter
     */
    fun createFormatter(type: WeatherFormatterType): WeatherFormatter {
        return when (type) {
            WeatherFormatterType.NOTIFICATION_DEFAULT -> WeatherDefaultNotificationFormatter()
            WeatherFormatterType.NOTIFICATION_SIMPLE -> WeatherSimpleNotificationFormatter()
            else -> throw IllegalArgumentException("Unknown type $type")
        }
    }
}

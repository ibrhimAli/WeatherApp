package com.musalasoft.weatherapp.notifications.ui

import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatterFactory
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatterType

/**
 * Factory for creation notification content updaters by type.
 */
object NotificationContentUpdaterFactory {
    /**
     * Create notification content updater for specified type.
     * @param type type of weather formatter
     * @return notification content updater for `type`
     */
    fun createNotificationContentUpdater(
        type: WeatherFormatterType
    ): NotificationContentUpdater {
        val formatter: WeatherFormatter = WeatherFormatterFactory.createFormatter(type)
        return when (type) {
            WeatherFormatterType.NOTIFICATION_DEFAULT -> DefaultNotificationContentUpdater(formatter)
            WeatherFormatterType.NOTIFICATION_SIMPLE -> SimpleNotificationContentUpdater(formatter)
            else -> throw IllegalArgumentException("Unknown type$type")
        }
    }

    /**
     * Check is content updater has appropriate class for specified type.
     * @param type type of weather formatter
     * @param contentUpdater content updater to check
     * @return `true` if content updater matches type and `false` if not.
     */
    fun doesContentUpdaterMatchType(
        type: WeatherFormatterType,
        contentUpdater: NotificationContentUpdater
    ): Boolean {
        return ((type === WeatherFormatterType.NOTIFICATION_DEFAULT
                && contentUpdater is DefaultNotificationContentUpdater)
                ||
                (type === WeatherFormatterType.NOTIFICATION_SIMPLE
                        && contentUpdater is SimpleNotificationContentUpdater))
    }
}

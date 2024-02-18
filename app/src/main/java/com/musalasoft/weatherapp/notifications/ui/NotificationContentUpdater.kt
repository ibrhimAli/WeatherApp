package com.musalasoft.weatherapp.notifications.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.CallSuper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.WeatherPresentation
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter

/**
 * Notification content updater populates notification with data from [WeatherPresentation].
 *
 * If [layout is custom][.isLayoutCustom], [.prepareRemoteView] and
 * [.updateNotification]
 * should be used. Otherwise use
 * [.updateNotification].
 */
abstract class NotificationContentUpdater {
    open val isLayoutCustom: Boolean
        /**
         * Returns `true` if notification has custom layout and `false` otherwise.
         * @return `true` if notification has custom layout and `false` otherwise
         */
        get() = false

    /**
     * Update notification with saved data and default view.
     * @param weatherPresentation data to show.
     * @param notification notification to update.
     * @param context android context.
     * @throws NullPointerException if any of parameters are `null`
     */
    @Throws(NullPointerException::class)
    open fun updateNotification(
        weatherPresentation: WeatherPresentation,
        notification: NotificationCompat.Builder,
        context: Context
    ) {
        setTemperatureAsIcon(weatherPresentation, notification, context)
    }

    /**
     * Create custom layout for notification.
     * @param context Android context
     * @return custom layout for notification
     * @throws NullPointerException if `context` is `null`
     */
    // cannot make PowerMock work with Robolectric so have to add updateNotification method
    // with RemoteViews as parameter
    @Throws(NullPointerException::class)
    open fun prepareRemoteView(context: Context): RemoteViews {
        throw UnsupportedOperationException("prepareRemoteView is not implemented")
    }

    /**
     * Update notification with saved data and custom view returned by
     * [.prepareRemoteView].
     * @param weatherPresentation data to show.
     * @param notification notification to update.
     * @param notificationLayout custom notification layout.
     * @param context android context.
     * @throws NullPointerException if any of parameters are `null`
     */
    @CallSuper
    @Throws(NullPointerException::class)
    open fun updateNotification(
        weatherPresentation: WeatherPresentation,
        notification: NotificationCompat.Builder,
        notificationLayout: RemoteViews,
        context: Context
    ) {
        setTemperatureAsIcon(weatherPresentation, notification, context)
    }

    // TODO add tests
    private fun setTemperatureAsIcon(
        weatherPresentation: WeatherPresentation,
        notification: NotificationCompat.Builder,
        context: Context
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val icon: IconCompat
            icon = if (weatherPresentation.shouldShowTemperatureInStatusBar()) {
                val color = ContextCompat.getColor(context, R.color.notification_icon_color)
                val statusBarIcon: Bitmap = WeatherFormatter.getTemperatureAsBitmap(
                    context,
                    weatherPresentation.getWeather(),
                    weatherPresentation.temperatureUnits,
                    color
                )
                IconCompat.createWithBitmap(statusBarIcon)
            } else {
                IconCompat.createWithResource(context, DEFAULT_NOTIFICATION_ICON)
            }
            notification.setSmallIcon(icon)
        } else {
            notification.setSmallIcon(DEFAULT_NOTIFICATION_ICON)
        }
    }

    companion object {
        val DEFAULT_NOTIFICATION_ICON: Int = R.drawable.cloud
    }
}
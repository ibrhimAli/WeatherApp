package com.musalasoft.weatherapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.models.WeatherPresentation
import com.musalasoft.weatherapp.notifications.repository.WeatherRepository
import com.musalasoft.weatherapp.notifications.ui.NotificationContentUpdater
import com.musalasoft.weatherapp.notifications.ui.NotificationContentUpdaterFactory
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatterType
import java.util.concurrent.Executors

/**
 * Service for showing and updating notification.
 */
class WeatherNotificationService : Service() {
    private var notificationManager: NotificationManagerCompat? = null
    private var notification: NotificationCompat.Builder? = null
    private var contentUpdater: NotificationContentUpdater? = null
    private var repository: WeatherRepository? = null
    private var repositoryListener: WeatherRepository.RepositoryListener? = null
    override fun onCreate() {
        createNotificationChannelIfNeeded(this)
        notificationManager = NotificationManagerCompat.from(this)
        val pendingIntent = notificationTapPendingIntent
        configureNotification(pendingIntent)
        startForeground(WEATHER_NOTIFICATION_ID, notification!!.build())
        repository = WeatherRepository(this, Executors.newSingleThreadExecutor())
        repositoryListener = object : WeatherRepository.RepositoryListener {
            override fun onChange(newData: WeatherPresentation) {
                updateNotification(newData)
            }
        }
        repository!!.observeWeather(repositoryListener!!)
    }

    // catch update of system's dark theme flags
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (repository != null) {
            updateNotification(repository!!.weather)
        }
    }

    private fun configureNotification(pendingIntent: PendingIntent?) {
        notification = NotificationCompat.Builder(this, WEATHER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(NotificationContentUpdater.DEFAULT_NOTIFICATION_ICON)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notification!!
                .setDefaults(0)
                .setVibrate(null)
                .setSound(null)
                .setLights(0, 0, 0)
        }
    }

    private val notificationTapPendingIntent: PendingIntent?
        /**
         * Create pending intent to open [MainActivity]
         * @return pending intent to open [MainActivity]
         */
        get() {
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addNextIntentWithParentStack(mainActivityIntent)
            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE)
        }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        if (notificationManager != null) notificationManager!!.cancel(WEATHER_NOTIFICATION_ID)
        stopForeground(true)
        if (repository != null) {
            repositoryListener = null
            repository!!.clear()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Put data into notification.
     */
    private fun updateNotification(weatherPresentation: WeatherPresentation) {
        Log.e(TAG, "notification update: " + weatherPresentation.toString())
        val updater: NotificationContentUpdater? = getContentUpdater(weatherPresentation.getType())
        if (updater!!.isLayoutCustom) {
            val layout: RemoteViews = updater.prepareRemoteView(this)
            updater.updateNotification(weatherPresentation, notification!!, layout, this)
        } else {
            updater.updateNotification(weatherPresentation, notification!!, this)
        }
        notificationManager!!.notify(WEATHER_NOTIFICATION_ID, notification!!.build())
    }

    @Synchronized
    private fun getContentUpdater(
        type: WeatherFormatterType
    ): NotificationContentUpdater? {
        if (contentUpdater == null
            || !NotificationContentUpdaterFactory.doesContentUpdaterMatchType(type, contentUpdater!!)
        ) {
            contentUpdater =
                NotificationContentUpdaterFactory.createNotificationContentUpdater(type)
        }
        return contentUpdater
    }

    companion object {
        private const val TAG = "WeatherNotificationServ"
        var WEATHER_NOTIFICATION_ID = 1
        private const val WEATHER_NOTIFICATION_CHANNEL_ID = "weather_notification_channel"

        /**
         * Create Notification Channels added in Android O to let a user configure notification
         * per channel and not by app.
         */
        private fun createNotificationChannelIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.channel_name)
                val channel = NotificationChannel(
                    WEATHER_NOTIFICATION_CHANNEL_ID,
                    name, NotificationManager.IMPORTANCE_LOW
                )
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setShowBadge(false)
                val notificationManager = context.getSystemService(
                    NotificationManager::class.java
                )
                notificationManager?.createNotificationChannel(channel)
            }
        }

        /**
         * Update Notification Channels if it has been created.
         * @param context Android context
         */
        fun updateNotificationChannelIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(
                    NotificationManager::class.java
                )
                if (notificationManager != null
                    && notificationManager.getNotificationChannel(WEATHER_NOTIFICATION_CHANNEL_ID) != null
                ) {
                    createNotificationChannelIfNeeded(context)
                }
            }
        }

        /**
         * Start foreground service to show and update weather notification.
         *
         * @param context context to create [Intent]
         */
        fun start(context: Context) {
            val intent = Intent(
                context,
                WeatherNotificationService::class.java
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop service and hide notification.
         *
         * @param context Android context
         */
        fun stop(context: Context) {
            val intent = Intent(
                context,
                WeatherNotificationService::class.java
            )
            context.stopService(intent)
        }
    }
}
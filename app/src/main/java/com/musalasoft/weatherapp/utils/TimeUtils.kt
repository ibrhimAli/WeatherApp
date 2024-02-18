package com.musalasoft.weatherapp.utils

import com.musalasoft.weatherapp.models.ImmutableWeather
import com.musalasoft.weatherapp.models.Weather
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun isDayTime(W: Weather?, Cal: Calendar): Boolean {
        if (W?.sunrise == null || W.sunset == null) return false
        val day: Boolean
        day = run {
            val currentTime = Calendar.getInstance().time // Cal is always set to midnight
            // then get real time
            currentTime.after(W.sunrise) && currentTime.before(W.sunset)
        }
        return day
    }

    /**
     * Returns `true` if now is between sunrise and sunset and `false` otherwise.
     * <br></br>
     * If sunrise and/or sunset is wrong, `true` will be returned.
     * @param weather weather information
     * @return is now between sunrise and sunset
     */
    fun isDayTime(weather: ImmutableWeather): Boolean {
        if (weather.sunrise < 0 || weather.sunset < 0) return true
        val sunrise = Date(TimeUnit.SECONDS.convert(weather.sunrise, TimeUnit.MILLISECONDS))
        val sunset = Date(TimeUnit.SECONDS.convert(weather.sunset, TimeUnit.MILLISECONDS))
        val isDay: Boolean
        val currentTime = Calendar.getInstance().time
        isDay = currentTime.after(sunrise) && currentTime.before(sunset)
        return isDay
    }
}

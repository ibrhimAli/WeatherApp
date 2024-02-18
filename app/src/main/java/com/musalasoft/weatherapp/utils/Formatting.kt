package com.musalasoft.weatherapp.utils

import android.content.Context
import com.musalasoft.weatherapp.utils.formatters.WeatherFormatter

class Formatting(private val context: Context) {
    fun getWeatherIcon(actualId: Int, isDay: Boolean): String {
        return WeatherFormatter.getWeatherIconAsText(actualId, isDay, context)
    }
}

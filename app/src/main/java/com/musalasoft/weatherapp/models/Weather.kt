package com.musalasoft.weatherapp.models

import android.content.Context
import com.musalasoft.weatherapp.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

class Weather {
    var city: String? = null
    var cityId = 0
    var country: String? = null
    var date: Date? = null
    var temperature = 0.0
    var description: String? = null
    var wind = 0.0
    var windDirectionDegree: Double? = null
    var pressure = 0F
    var humidity = 0
    var rain = 0.0
    var weatherId = 0
    var lastUpdated: Long = 0
    var sunrise: Date? = null
        private set
    var sunset: Date? = null
        private set
    var lat = 0.0
    var lon = 0.0
    var uvIndex = 0.0
    var chanceOfPrecipitation = 0.0

    enum class WindDirection {
        // don't change order
        NORTH,
        NORTH_NORTH_EAST,
        NORTH_EAST,
        EAST_NORTH_EAST,
        EAST,
        EAST_SOUTH_EAST,
        SOUTH_EAST,
        SOUTH_SOUTH_EAST,
        SOUTH,
        SOUTH_SOUTH_WEST,
        SOUTH_WEST,
        WEST_SOUTH_WEST,
        WEST,
        WEST_NORTH_WEST,
        NORTH_WEST,
        NORTH_NORTH_WEST;

        fun getLocalizedString(context: Context): String {
            // usage of enum.ordinal() is not recommended, but whatever
            return context.resources.getStringArray(R.array.windDirections)[ordinal]
        }

        fun getArrow(context: Context): String {
            // usage of enum.ordinal() is not recommended, but whatever
            return context.resources.getStringArray(R.array.windDirectionArrows)[ordinal / 2]
        }

    }

    open fun byDegree(degree: Double, numberOfDirections: Int = WindDirection.entries.size): WindDirection {
        val directions = WindDirection.entries.toTypedArray()
        val availableNumberOfDirections = directions.size
        val direction = windDirectionDegreeToIndex(degree, numberOfDirections) * availableNumberOfDirections / numberOfDirections
        return directions[direction]
    }

    val windDirection: WindDirection
        get() = byDegree(windDirectionDegree!!)

    fun getWindDirection(numberOfDirections: Int): WindDirection {
        return byDegree(windDirectionDegree!!, numberOfDirections)
    }

    val isWindDirectionAvailable: Boolean
        get() = windDirectionDegree != null

    fun setSunrise(dateString: String) {
        try {
            setSunrise(Date(dateString.toLong() * 1000))
        } catch (e: Exception) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            try {
                setSunrise(inputFormat.parse(dateString))
            } catch (e2: ParseException) {
                setSunrise(Date()) // make the error somewhat obvious
                e2.printStackTrace()
            }
        }
    }

    fun setSunrise(date: Date?) {
        sunrise = date
    }

    fun setSunset(dateString: String) {
        try {
            setSunset(Date(dateString.toLong() * 1000))
        } catch (e: Exception) {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            try {
                setSunrise(inputFormat.parse(dateString))
            } catch (e2: ParseException) {
                setSunset(Date()) // make the error somewhat obvious
                e2.printStackTrace()
            }
        }
    }

    fun setSunset(date: Date?) {
        sunset = date
    }

    fun getNumDaysFrom(initialDate: Date?): Long {
        val initial = Calendar.getInstance()
        initial.time = initialDate
        initial[Calendar.MILLISECOND] = 0
        initial[Calendar.SECOND] = 0
        initial[Calendar.MINUTE] = 0
        initial[Calendar.HOUR_OF_DAY] = 0
        val me = Calendar.getInstance()
        me.time = date
        me[Calendar.MILLISECOND] = 0
        me[Calendar.SECOND] = 0
        me[Calendar.MINUTE] = 0
        me[Calendar.HOUR_OF_DAY] = 0
        return Math.round((me.timeInMillis - initial.timeInMillis) / 86400000.0)
    }

    companion object {
        // you may use values like 4, 8, etc. for numberOfDirections
        fun windDirectionDegreeToIndex(degree: Double, numberOfDirections: Int): Int {
            // to be on the safe side
            var degree = degree
            degree %= 360.0
            if (degree < 0) degree += 360.0
            degree += 180.0 / numberOfDirections // add offset to make North start from 0
            val direction = floor(degree * numberOfDirections / 360).toInt()
            return direction % numberOfDirections
        }

        fun byDegree(degree: Double, numberOfDirections: Int = WindDirection.entries.size): WindDirection {
            val directions = WindDirection.entries.toTypedArray()
            val availableNumberOfDirections = directions.size
            val direction = windDirectionDegreeToIndex(degree, numberOfDirections) * availableNumberOfDirections / numberOfDirections
            return directions[direction]
        }
    }
}

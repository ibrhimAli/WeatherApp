package com.musalasoft.weatherapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.musalasoft.weatherapp.R
import java.util.Locale

object UnitConvertor {
    fun convertTemperature(temperature: Float, sp: SharedPreferences): Float {
        val unit = sp.getString("unit", "°C")
        return convertTemperature(temperature, unit)
    }

    fun convertTemperature(temperature: Float, unit: String?): Float {
        val result: Float
        result = when (unit) {
            "°C" -> kelvinToCelsius(temperature)
            "°F" -> kelvinToFahrenheit(temperature)
            else -> temperature
        }
        return result
    }

    fun kelvinToCelsius(kelvinTemp: Float): Float {
        return kelvinTemp - 273.15f
    }

    fun kelvinToFahrenheit(kelvinTemp: Float): Float {
        return (kelvinTemp - 273.15f) * 1.8f + 32
    }

    fun convertRain(rain: Float, sp: SharedPreferences): Float {
        return if (sp.getString("lengthUnit", "mm") == "mm") {
            rain
        } else {
            rain / 25.4f
        }
    }

    fun getRainString(rain: Double, percentOfPrecipitation: Double, sp: SharedPreferences): String {
        val sb = StringBuilder()
        if (rain > 0) {
            sb.append(" (")
            val lengthUnit = sp.getString("lengthUnit", "mm")
            val isMetric = lengthUnit == "mm"
            if (rain < 0.1) {
                sb.append(if (isMetric) "<0.1" else "<0.01")
            } else if (isMetric) {
                sb.append(String.format(Locale.ENGLISH, "%.1f %s", rain, lengthUnit))
            } else {
                sb.append(String.format(Locale.ENGLISH, "%.2f %s", rain, lengthUnit))
            }
            if (percentOfPrecipitation > 0) {
                sb.append(", ").append(
                    String.format(
                        Locale.ENGLISH,
                        "%d%%",
                        (percentOfPrecipitation * 100).toInt()
                    )
                )
            }
            sb.append(")")
        }
        return sb.toString()
    }

    fun convertPressure(pressure: Float, sp: SharedPreferences): Float {
        return if (sp.getString("pressureUnit", "hPa") == "kPa") {
            pressure / 10
        } else if (sp.getString("pressureUnit", "hPa") == "mm Hg") {
            (pressure * 0.750061561303).toFloat()
        } else if (sp.getString("pressureUnit", "hPa") == "in Hg") {
            (pressure * 0.0295299830714).toFloat()
        } else {
            pressure
        }
    }

    fun convertPressure(pressure: Double, unit: String?): Double {
        val result: Double
        result = when (unit) {
            "kPa" -> pressure / 10
            "mm Hg" -> pressure * 0.750061561303
            "in Hg" -> pressure * 0.0295299830714
            else -> pressure
        }
        return result
    }

    fun convertWind(wind: Double, sp: SharedPreferences): Double {
        val result: Double
        val unit = sp.getString("speedUnit", "m/s")
        result = when (unit) {
            "kph" -> wind * 3.6
            "mph" -> wind * 2.23693629205
            "kn" -> wind * 1.943844
            "bft" -> convertWindIntoBFT(wind)
            else -> wind
        }
        return result
    }

    fun convertWind(wind: Double, unit: String?): Double {
        val result: Double
        result = when (unit) {
            "kph" -> wind * 3.6
            "mph" -> wind * 2.23693629205
            "kn" -> wind * 1.943844
            "bft" -> convertWindIntoBFT(wind)
            else -> wind
        }
        return result
    }

    private fun convertWindIntoBFT(wind: Double): Double {
        val result: Int
        result = if (wind < 0.3) {
            0 // Calm
        } else if (wind < 1.5) {
            1 // Light air
        } else if (wind < 3.3) {
            2 // Light breeze
        } else if (wind < 5.5) {
            3 // Gentle breeze
        } else if (wind < 7.9) {
            4 // Moderate breeze
        } else if (wind < 10.7) {
            5 // Fresh breeze
        } else if (wind < 13.8) {
            6 // Strong breeze
        } else if (wind < 17.1) {
            7 // High wind
        } else if (wind < 20.7) {
            8 // Gale
        } else if (wind < 24.4) {
            9 // Strong gale
        } else if (wind < 28.4) {
            10 // Storm
        } else if (wind < 32.6) {
            11 // Violent storm
        } else {
            12 // Hurricane
        }
        return result.toDouble()
    }

    fun convertUvIndexToRiskLevel(value: Double, context: Context): String {
        /* based on: https://en.wikipedia.org/wiki/Ultraviolet_index */
        return if (value >= 0.0 && value < 3.0) {
            context.getString(R.string.uvi_low)
        } else if (value >= 3.0 && value < 6.0) {
            context.getString(R.string.uvi_moderate)
        } else if (value >= 6.0 && value < 8.0) {
            context.getString(R.string.uvi_high)
        } else if (value >= 8.0 && value < 11.0) {
            context.getString(R.string.uvi_very_high)
        } else if (value >= 11.0) {
            context.getString(R.string.uvi_extreme)
        } else {
            context.getString(R.string.uvi_no_info)
        }
    }

    fun getBeaufortName(wind: Int, context: Context): String {
        return if (wind == 0) {
            context.getString(R.string.beaufort_calm)
        } else if (wind == 1) {
            context.getString(R.string.beaufort_light_air)
        } else if (wind == 2) {
            context.getString(R.string.beaufort_light_breeze)
        } else if (wind == 3) {
            context.getString(R.string.beaufort_gentle_breeze)
        } else if (wind == 4) {
            context.getString(R.string.beaufort_moderate_breeze)
        } else if (wind == 5) {
            context.getString(R.string.beaufort_fresh_breeze)
        } else if (wind == 6) {
            context.getString(R.string.beaufort_strong_breeze)
        } else if (wind == 7) {
            context.getString(R.string.beaufort_high_wind)
        } else if (wind == 8) {
            context.getString(R.string.beaufort_gale)
        } else if (wind == 9) {
            context.getString(R.string.beaufort_strong_gale)
        } else if (wind == 10) {
            context.getString(R.string.beaufort_storm)
        } else if (wind == 11) {
            context.getString(R.string.beaufort_violent_storm)
        } else {
            context.getString(R.string.beaufort_hurricane)
        }
    }
}

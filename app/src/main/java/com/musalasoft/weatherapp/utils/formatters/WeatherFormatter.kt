package com.musalasoft.weatherapp.utils.formatters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.ImmutableWeather

/**
 * Converter from raw [wather info][ImmutableWeather] into strings to show to a user.
 * <br></br>
 * NOTE: default implementation for all methods is to throw [UnsupportedOperationException].
 */
@Suppress("unused")
abstract class WeatherFormatter {
    /**
     * Check is `weather` has enough valid data to show all necessary weather information to
     * a user or `no data` should be shown.
     * <br></br>
     * NOTE: Derived class could have more specified checks and this implementation should return
     * `false` if any of data that can be displayed is not valid.
     * @param weather weather information
     * @return `true` if weather information valid and `false` if `no data`
     * should be shown
     */
    abstract fun isEnoughValidData(weather: ImmutableWeather): Boolean

    /**
     * Returns temperature with units if needed.
     * @param weather weather information
     * @param temperatureUnit temperature units
     * @param roundedTemperature if `true` round temperature and show as integer
     * @return temperature with units if needed
     */
    open fun getTemperature(
        weather: ImmutableWeather,
        temperatureUnit: String,
        roundedTemperature: Boolean
    ): String {
        throw UnsupportedOperationException("getTemperature hasn't been implemented")
    }

    /**
     * Returns formatted weather description.
     * @param weather weather information
     * @return formatted weather description
     */
    open fun getDescription(weather: ImmutableWeather): String {
        throw UnsupportedOperationException("getDescription hasn't been implemented")
    }

    /**
     * Returns weather wind title, wind speed in specified units and wind direction in specified
     * format.
     * @param weather weather information
     * @param units wind speed units
     * @param directionFormat wind direction format
     * @param context android context
     * @return formatted wind
     */
    // TODO rewrite with enums instead of Strings
    open fun getWind(
        weather: ImmutableWeather, units: String,
        directionFormat: String, context: Context
    ): String {
        throw UnsupportedOperationException("getWind hasn't been implemented")
    }

    /**
     * Returns pressure title, pressure in specified units and units.
     * @param weather weather information
     * @param units pressure units
     * @param context android context
     * @return formatted pressure
     */
    // TODO rewrite units with enum
    open fun getPressure(
        weather: ImmutableWeather, units: String,
        context: Context
    ): String {
        throw UnsupportedOperationException("getPressure hasn't been implemented")
    }

    /**
     * Returns humidity title, humidity value and per cent symbol.
     * @param weather weather information
     * @param context android context
     * @return formatted humidity
     */
    open fun getHumidity(weather: ImmutableWeather, context: Context): String {
        throw UnsupportedOperationException("getHumidity hasn't been implemented")
    }

    /**
     * Returns weather icon as [Bitmap].
     * @param weather weather information
     * @param context android context
     * @return weather icon as [Bitmap]
     */
    open fun getWeatherIconAsBitmap(
        weather: ImmutableWeather,
        context: Context
    ): Bitmap {
        throw UnsupportedOperationException("getWeatherIconAsBitmap hasn't been implemented")
    }

    /**
     * Returns weather icon as [String].
     * @param weather weather information
     * @param context android context
     * @return weather icon as [String]
     */
    fun getWeatherIconAsText(
        weather: ImmutableWeather,
        context: Context
    ): String {
        throw UnsupportedOperationException("getWeatherIconAsText hasn't been implemented")
    }

    companion object {
        const val DEFAULT_ICON_TEXT_SIZE = 24
        const val MIN_ICON_TEXT_SIZE = 14

        /**
         * Returns weather icon as [String].
         * @param weatherId weather icon id
         * @param isDay `true` if should be chosen icon for day and `false` for night
         * @param context android context
         * @return weather icon as [String]
         */
        // TODO static is temporary solution to avoid code duplication. Should be moved in another
        // class and retrieved through DI.
        fun getWeatherIconAsText(
            weatherId: Int, isDay: Boolean,
            context: Context
        ): String {
            val id = weatherId / 100
            var icon = ""
            if (id == 2) {
                // thunderstorm
                icon = when (weatherId) {
                    210, 211, 212, 221 -> context.getString(R.string.weather_lightning)
                    200, 201, 202, 230, 231, 232 -> context.getString(R.string.weather_thunderstorm)
                    else -> context.getString(R.string.weather_thunderstorm)
                }
            } else if (id == 3) {
                // drizzle/sprinkle
                icon = when (weatherId) {
                    302, 311, 312, 314 -> context.getString(R.string.weather_rain)
                    310 -> context.getString(R.string.weather_rain_mix)
                    313 -> context.getString(R.string.weather_showers)
                    300, 301, 321 -> context.getString(R.string.weather_sprinkle)
                    else -> context.getString(R.string.weather_sprinkle)
                }
            } else if (id == 5) {
                // rain
                icon = when (weatherId) {
                    500 -> context.getString(R.string.weather_sprinkle)
                    511 -> context.getString(R.string.weather_rain_mix)
                    520, 521, 522 -> context.getString(R.string.weather_showers)
                    531 -> context.getString(R.string.weather_storm_showers)
                    501, 502, 503, 504 -> context.getString(R.string.weather_rain)
                    else -> context.getString(R.string.weather_rain)
                }
            } else if (id == 6) {
                // snow
                icon = when (weatherId) {
                    611 -> context.getString(R.string.weather_sleet)
                    612, 613, 615, 616, 620 -> context.getString(R.string.weather_rain_mix)
                    600, 601, 602, 621, 622 -> context.getString(R.string.weather_snow)
                    else -> context.getString(R.string.weather_snow)
                }
            } else if (id == 7) {
                // atmosphere
                icon = when (weatherId) {
                    711 -> context.getString(R.string.weather_smoke)
                    721 -> context.getString(R.string.weather_day_haze)
                    731, 761, 762 -> context.getString(R.string.weather_dust)
                    751 -> context.getString(R.string.weather_sandstorm)
                    771 -> context.getString(R.string.weather_cloudy_gusts)
                    781 -> context.getString(R.string.weather_tornado)
                    701, 741 -> context.getString(R.string.weather_fog)
                    else -> context.getString(R.string.weather_fog)
                }
            } else if (id == 8) {
                // clear sky or cloudy
                icon = when (weatherId) {
                    800 -> if (isDay) context.getString(R.string.weather_day_sunny) else context.getString(
                        R.string.weather_night_clear
                    )

                    801, 802 -> if (isDay) context.getString(R.string.weather_day_cloudy) else context.getString(
                        R.string.weather_night_alt_cloudy
                    )

                    803, 804 -> context.getString(R.string.weather_cloudy)
                    else -> context.getString(R.string.weather_cloudy)
                }
            } else if (id == 9) {
                icon = when (weatherId) {
                    900 -> context.getString(R.string.weather_tornado)
                    901 -> context.getString(R.string.weather_storm_showers)
                    902 -> context.getString(R.string.weather_hurricane)
                    903 -> context.getString(R.string.weather_snowflake_cold)
                    904 -> context.getString(R.string.weather_hot)
                    905 -> context.getString(R.string.weather_windy)
                    906 -> context.getString(R.string.weather_hail)
                    957 -> context.getString(R.string.weather_strong_wind)
                    else -> context.getString(R.string.weather_strong_wind)
                }
            }
            return icon
        }

        /**
         * Returns weather icon as [Bitmap].
         * @param context android context
         * @param text weather icon as String
         * @param color text color (not a color resource)
         * @return weather icon as [Bitmap]
         * @see .getWeatherIconAsText
         */
        // TODO static is temporary solution to avoid code duplication.  Should be moved in another
        // class and retrieved through DI.
        fun getWeatherIconAsBitmap(
            context: Context, text: String,
            color: Int
        ): Bitmap {
            val myBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444)
            val myCanvas = Canvas(myBitmap)
            val paint = getPaint(color)
            val clock = Typeface.createFromAsset(context.assets, "fonts/weather.ttf")
            paint.setTypeface(clock)
            paint.textSize = 150f
            myCanvas.drawText(text, 128f, 180f, paint)
            return myBitmap
        }

        /**
         * Returns weather icon as [Bitmap].
         * @param context android context
         * @param weather weather information
         * @param temperatureUnit temperature unit
         * @param color text color (not a color resource)
         * @return weather icon as [Bitmap]
         */
        // TODO static is temporary solution to avoid code duplication. Should be moved in another
        // class and retrieved through DI.
        fun getTemperatureAsBitmap(
            context: Context,
            weather: ImmutableWeather,
            temperatureUnit: String,
            color: Int
        ): Bitmap {
            val displayMetrics = context.resources.displayMetrics
            val size = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_ICON_TEXT_SIZE.toFloat(),
                displayMetrics
            )
            val oneDp = size / DEFAULT_ICON_TEXT_SIZE
            val minTextSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                (MIN_ICON_TEXT_SIZE - 1).toFloat(),
                displayMetrics
            )
            val bitmap = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_4444)
            val temperature =
                Math.round(weather.getTemperature(temperatureUnit)).toInt()
            drawTemperature(
                bitmap,
                getTemperaturePaint(color),
                temperature,
                size,
                oneDp,
                minTextSize
            )
            return bitmap
        }

        private fun drawTemperature(
            bitmap: Bitmap,
            paint: Paint,
            temperature: Int,
            size: Float, oneDp: Float, minTextSize: Float
        ) {
            val canvas = Canvas(bitmap)
            var textSize = size
            val text = "$temperature°"
            val bounds = Rect()
            var measuredWidth: Float
            do {
                paint.textSize = textSize
                paint.getTextBounds(text, 0, text.length, bounds)
                measuredWidth = bounds.width().toFloat()
                textSize -= oneDp
            } while (measuredWidth > size && textSize > minTextSize)
            val textHeight = bounds.height().toFloat()
            val verticalPadding = (size - textHeight) / 2f
            val x = size / 2f
            val y = verticalPadding + textHeight
            canvas.drawText(text, x, y, paint)
        }

        private fun getTemperaturePaint(color: Int): Paint {
            val paint = getPaint(color)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                paint.letterSpacing = -0.05f
            }
            //paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.DEFAULT_BOLD)
            return paint
        }

        private fun getPaint(color: Int): Paint {
            val paint = Paint()
            paint.isAntiAlias = true
            paint.isSubpixelText = true
            paint.style = Paint.Style.FILL_AND_STROKE
            paint.color = color
            paint.textAlign = Paint.Align.CENTER
            return paint
        }
    }
}
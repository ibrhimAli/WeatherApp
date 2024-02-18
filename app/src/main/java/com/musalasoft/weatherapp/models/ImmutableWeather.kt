package com.musalasoft.weatherapp.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import com.musalasoft.weatherapp.utils.Formatting
import com.musalasoft.weatherapp.utils.UnitConvertor
import org.json.JSONException
import org.json.JSONObject

/**
 * Weather information.
 * <br></br>
 * To create pass json into [.fromJson]. For default value use
 * [ImmutableWeather.EMPTY].
 */
// TODO add rain
class ImmutableWeather : Parcelable {
    /**
     * Valid numbers of wind directions:
     *
     *  * [.WIND_DIRECTIONS_SIMPLE] - base four directions: north, east, south and west.
     *  * [.WIND_DIRECTIONS_ARROWS] - eight directions for arrows.
     *  * [.WIND_DIRECTIONS_MAX] - all sixteen directions.
     *
     */
    @IntDef(*[WIND_DIRECTIONS_SIMPLE, WIND_DIRECTIONS_ARROWS, WIND_DIRECTIONS_MAX])
    annotation class NumberOfWindDirections

    /**
     * Returns temperature in kelvins.
     * <br></br>
     * Default value for invalid data: [Float.MIN_VALUE].
     * @return temperature in kelvins
     * @see .getTemperature
     */
    var temperature = Float.MIN_VALUE
        private set

    /**
     * Returns pressure in default unit (hPa/mBar).
     * <br></br>
     * Default value for invalid data: [Double.MIN_VALUE].
     * @return pressure in hPa/mBar
     * @see .getPressure
     */
    var pressure = Double.MIN_VALUE
        private set

    /**
     * Returns humidity in per cents.
     * <br></br>
     * Default value for invalid data: -1.
     * @return humidity in per cents
     */
    var humidity = -1
        private set

    /**
     * Returns wind speed in meter/sec.
     * <br></br>
     * Default value for invalid data: [Double.MIN_VALUE].
     * @return wind speed in meter/sec
     * @see .getWindSpeed
     */
    var windSpeed = Double.MIN_VALUE
        private set
    private var windDirection: Weather.WindDirection? = null

    /**
     * Returns sunrise time as UNIX timestamp.
     * <br></br>
     * Default value for invalid data: -1.
     * @return sunrise time as UNIX timestamp
     */
    var sunrise = -1L
        private set

    /**
     * Returns sunset time as UNIX timestamp.
     * <br></br>
     * Default value for invalid data: -1.
     * @return sunset time as UNIX timestamp
     */
    var sunset = -1L
        private set

    /**
     * Returns city name.
     * <br></br>
     * Default value for invalid data: empty string.
     * @return city name
     */
    var city = ""
        private set

    /**
     * Returns country code.
     * <br></br>
     * Default value for invalid data: empty string.
     * @return country code
     */
    var country = ""
        private set

    /**
     * Returns weather description.
     * <br></br>
     * Default value for invalid data: empty string.
     * @return weather description.
     */
    var description = ""
        private set

    /**
     * Returns weather id for formatting weather icon.
     * <br></br>
     * Default value for invalid data: -1.
     * @return weather id
     * @see Formatting.getWeatherIcon
     */
    var weatherIcon = -1
        private set

    /**
     * Returns time when this data has been created as timestamp in milliseconds.
     * <br></br>
     * Default value for invalid data: -1.
     * @return data creation timestamp in milliseconds
     */
    var lastUpdate = -1L
        private set

    private constructor()

    /**
     * Returns temperature in specified [unit].
     * <br></br>
     * Default value for invalid data: [Float.MIN_VALUE].
     * @param unit resulted unit
     * @return temperature in specified unit
     * @throws NullPointerException if `unit` is null
     */
    // TODO rewrite units as enum
    @Throws(NullPointerException::class)
    fun getTemperature(unit: String): Float {
        if (unit == null) throw NullPointerException("unit should not be null")
        val result: Float
        result =
            if (temperature == Float.MIN_VALUE) temperature else UnitConvertor.convertTemperature(
                temperature,
                unit
            )
        return result
    }

    /**
     * Returns **rounded** temperature in specified [unit].
     * <br></br>
     * Default value for invalid data: [Integer.MIN_VALUE].
     * @param unit resulted unit
     * @return rounded temperature in specified unit
     * @throws NullPointerException if `unit` is null
     */
    // TODO rewrite units as enum
    @Throws(NullPointerException::class)
    fun getRoundedTemperature(unit: String): Int {
        if (unit == null) throw NullPointerException("unit should not be null")
        val result: Int
        result =
            if (temperature == Float.MIN_VALUE) Int.MIN_VALUE else {
                val convertedTemperature: Float =
                    UnitConvertor.convertTemperature(temperature, unit)
                Math.round(convertedTemperature)
            }
        return result
    }

    /**
     * Returns pressure in specified [unit].
     * <br></br>
     * Default value for invalid data: [Double.MIN_VALUE].
     * @param unit resulted unit
     * @return pressure in specified unit
     * @throws NullPointerException if `unit` is null
     */
    // TODO rewrite units as enum
    @Throws(NullPointerException::class)
    fun getPressure(unit: String): Double {
        if (unit == null) throw NullPointerException("unit should not be null")
        val result: Double
        result = if (pressure == Double.MIN_VALUE) pressure else UnitConvertor.convertPressure(
            pressure,
            unit
        )
        return result
    }

    /**
     * Returns wind speed in specified `unit`.
     * <br></br>
     * Default value for invalid data: [Double.MIN_VALUE].
     * @param unit resulted unit
     * @return wind speed in specified unit
     * @throws NullPointerException if `unit` is null
     */
    @Throws(NullPointerException::class)
    fun getWindSpeed(unit: String): Double {
        if (unit == null) throw NullPointerException("unit should not be null")
        val result: Double
        result = if (windSpeed == Double.MIN_VALUE) windSpeed else UnitConvertor.convertWind(
            windSpeed,
            unit
        )
        return result
    }

    /**
     * Returns wind direction.
     * <br></br>
     * Default value for invalid data: `null`.
     * @return wind direction
     * @see Weather.WindDirection
     */
    fun getWindDirection(): Weather.WindDirection? {
        return windDirection
    }

    /**
     * Returns wind direction scaled by specified maximum possible directions.
     * <br></br>
     * Default value for invalid data: `null`.
     * @param maxDirections maximum possible directions
     * @return wind direction scaled by `maxDirections`
     * @see NumberOfWindDirections
     */
    fun getWindDirection(@NumberOfWindDirections maxDirections: Int): Weather.WindDirection? {
        val result: Weather.WindDirection?
        result = if (windDirection == null) null else {
            val diff: Int = Weather.WindDirection.values().size / maxDirections - 1
            Weather.WindDirection.values().get(windDirection!!.ordinal - diff)
        }
        return result
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ImmutableWeather
        if (java.lang.Float.compare(that.temperature, temperature) != 0) return false
        if (java.lang.Double.compare(that.pressure, pressure) != 0) return false
        if (humidity != that.humidity) return false
        if (java.lang.Double.compare(that.windSpeed, windSpeed) != 0) return false
        if (sunrise != that.sunrise) return false
        if (sunset != that.sunset) return false
        if (weatherIcon != that.weatherIcon) return false
        if (lastUpdate != that.lastUpdate) return false
        if (windDirection !== that.windDirection) return false
        if (city != that.city) return false
        return if (country != that.country) false else description == that.description
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long
        result = if (temperature != +0.0f) java.lang.Float.floatToIntBits(temperature) else 0
        temp = java.lang.Double.doubleToLongBits(pressure)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        result = 31 * result + humidity
        temp = java.lang.Double.doubleToLongBits(windSpeed)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        result = 31 * result + if (windDirection != null) windDirection.hashCode() else 0
        result = 31 * result + (sunrise xor (sunrise ushr 32)).toInt()
        result = 31 * result + (sunset xor (sunset ushr 32)).toInt()
        result = 31 * result + city.hashCode()
        result = 31 * result + country.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + weatherIcon
        result = 31 * result + (lastUpdate xor (lastUpdate ushr 32)).toInt()
        return result
    }

    override fun toString(): String {
        return "ImmutableWeather{" +
                "temperature=" + temperature +
                ", pressure=" + pressure +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", windDirection=" + windDirection +
                ", sunrise=" + sunrise +
                ", sunset=" + sunset +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", description='" + description + '\'' +
                ", weatherIcon=" + weatherIcon +
                ", lastUpdate=" + lastUpdate +
                '}'
    }

    // Parcelable implementation
    protected constructor(`in`: Parcel) {
        temperature = `in`.readFloat()
        pressure = `in`.readDouble()
        humidity = `in`.readInt()
        windSpeed = `in`.readDouble()
        val direction = `in`.readInt()
        windDirection =
            if (direction < 0 || direction >= Weather.WindDirection.values().size) null else Weather.WindDirection.values()
                .get(direction)
        sunrise = `in`.readLong()
        sunset = `in`.readLong()
        city = `in`.readString()!!
        country = `in`.readString()!!
        description = `in`.readString()!!
        weatherIcon = `in`.readInt()
        lastUpdate = `in`.readLong()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeFloat(temperature)
        dest.writeDouble(pressure)
        dest.writeInt(humidity)
        dest.writeDouble(windSpeed)
        if (windDirection == null) dest.writeInt(Int.MIN_VALUE) else dest.writeInt(windDirection!!.ordinal)
        dest.writeLong(sunrise)
        dest.writeLong(sunset)
        dest.writeString(city)
        dest.writeString(country)
        dest.writeString(description)
        dest.writeInt(weatherIcon)
        dest.writeLong(lastUpdate)
    }

    companion object {
        /**
         * Value object for unknown weather (like there is no information to parse).
         */
        val EMPTY = ImmutableWeather()

        /** Base four directions: north, east, south and west.  */
        const val WIND_DIRECTIONS_SIMPLE = 4

        /** Eight directions for arrows.  */
        const val WIND_DIRECTIONS_ARROWS = 8

        /** All sixteen directions.  */
        const val WIND_DIRECTIONS_MAX = 16

        /**
         * Parse OpenWeatherMap response json and initialize object with weather information.
         * <br></br>
         * If `json` is empty or has empty object (i.e. `"{}"`), [.EMPTY] will be
         * returned.
         *
         * @param json json with weather information from OWM.
         * @param lastUpdate time of retrieving response in milliseconds.
         * @return parsed OWM response
         * @throws NullPointerException if `json` is null.
         */
        @Throws(NullPointerException::class)
        fun fromJson(json: String, lastUpdate: Long): ImmutableWeather {
            if (json == null) throw NullPointerException("json should not be null")
            return try {
                val reader = JSONObject(json)
                if (reader.length() == 0) EMPTY else {
                    val result = ImmutableWeather()
                    result.lastUpdate = lastUpdate
                    val main = reader.optJSONObject("main")
                    // temperature
                    result.temperature = getFloat("temp", Float.MIN_VALUE, main)
                    // pressure
                    result.pressure = getDouble("pressure", Double.MIN_VALUE, main)
                    // humidity
                    result.humidity = getInt("humidity", -1, main)
                    val wind = reader.optJSONObject("wind")
                    // wind speed
                    result.windSpeed = getDouble("speed", Double.MIN_VALUE, wind)
                    // wind direction
                    val degree = getInt("deg", Int.MIN_VALUE, wind)
                    result.windDirection =
                        if (degree == Int.MIN_VALUE) null else Weather.byDegree(degree.toDouble())
                    val weather = reader.optJSONArray("weather")
                    val todayWeather = weather?.optJSONObject(0)
                    // description
                    if (todayWeather != null) result.description =
                        todayWeather.optString("description", "")
                    result.weatherIcon = getInt("id", -1, todayWeather)
                    if (result.weatherIcon < -1) result.weatherIcon = -1
                    val sys = reader.optJSONObject("sys")
                    // country
                    if (sys != null) result.country = sys.optString("country", "")
                    // sunrise
                    result.sunrise = getTimestamp("sunrise", -1L, sys)
                    // sunset
                    result.sunset = getTimestamp("sunset", -1L, sys)

                    // city
                    result.city = reader.optString("name", "")
                    result
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                EMPTY
            }
        }

        private fun getFloat(key: String, def: Float, jsonObject: JSONObject?): Float {
            val result: Float
            result = if (jsonObject != null && jsonObject.has(key)) {
                try {
                    jsonObject.getString("temp").toFloat()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    def
                } catch (e: JSONException) {
                    e.printStackTrace()
                    def
                }
            } else {
                def
            }
            return result
        }

        private fun getDouble(
            key: String,
            def: Double,
            jsonObject: JSONObject?
        ): Double {
            val result: Double
            result = if (jsonObject != null && jsonObject.has(key)) {
                try {
                    jsonObject.getDouble(key)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    def
                }
            } else {
                def
            }
            return result
        }

        private fun getInt(key: String, def: Int, jsonObject: JSONObject?): Int {
            val result: Int
            result = if (jsonObject != null && jsonObject.has(key)) {
                try {
                    jsonObject.getInt(key)
                } catch (e: JSONException) {
                    def
                }
            } else {
                def
            }
            return result
        }

        private fun getTimestamp(key: String, def: Long, jsonObject: JSONObject?): Long {
            var result: Long
            if (jsonObject != null && jsonObject.has(key)) {
                try {
                    result = jsonObject.getLong(key)
                    if (result < 0) result = def
                } catch (e: JSONException) {
                    e.printStackTrace()
                    result = def
                }
            } else {
                result = def
            }
            return result
        }

        @JvmField
        val CREATOR: Parcelable.Creator<ImmutableWeather?> =
            object : Parcelable.Creator<ImmutableWeather?> {
                override fun createFromParcel(`in`: Parcel): ImmutableWeather {
                    return ImmutableWeather(`in`)
                }

                override fun newArray(size: Int): Array<ImmutableWeather?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
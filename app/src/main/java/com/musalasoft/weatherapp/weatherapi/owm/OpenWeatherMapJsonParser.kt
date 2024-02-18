package com.musalasoft.weatherapp.weatherapi.owm

import android.util.Log
import com.musalasoft.weatherapp.models.Weather
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import java.util.Locale

object OpenWeatherMapJsonParser {
    @Throws(JSONException::class)
    fun convertJsonToWeatherList(citiesString: String?): List<Weather> {
        val citiesWithWeather: MutableList<Weather> = ArrayList<Weather>()
        val weatherObject = JSONObject(citiesString!!)
        val weatherArray = weatherObject.getJSONArray("list")
        val cityObject = weatherObject.getJSONObject("city")
        val cityId = cityObject.getInt("id")
        val cityName = cityObject.getString("name")
        val country = cityObject.getString("country")
        for (i in 0 until weatherArray.length()) {
            val currentWeatherObject = weatherArray.getJSONObject(i)
            val weather: Weather = getWeatherFromJsonObject(currentWeatherObject)
            setSunsetAndSunrise(weather, cityObject)
            setCityAndCountry(weather, cityId, cityName, country)
            val coordinatesObject = cityObject.getJSONObject("coord")
            setCoordinates(weather, coordinatesObject)
            weather.chanceOfPrecipitation = currentWeatherObject.optDouble("pop", 0.0)
            citiesWithWeather.add(weather)
        }
        return citiesWithWeather
    }

    @Throws(JSONException::class)
    fun convertJsonToWeather(weatherString: String?): Weather {
        val weatherObject = JSONObject(weatherString!!)
        val weather: Weather = getWeatherFromJsonObject(weatherObject)
        val systemObject = weatherObject.getJSONObject("sys")
        setSunsetAndSunrise(weather, systemObject)
        val cityId = weatherObject.getInt("id")
        val cityName = weatherObject.getString("name")
        val country = systemObject.getString("country")
        setCityAndCountry(weather, cityId, cityName, country)
        val coordinatesObject = weatherObject.getJSONObject("coord")
        setCoordinates(weather, coordinatesObject)
        return weather
    }

    @Throws(JSONException::class)
    fun convertJsonToUVIndex(uviString: String?): Double {
        val jsonObject = JSONObject(uviString!!)
        return jsonObject.getDouble("value")
    }

    @Throws(JSONException::class)
    private fun getWeatherFromJsonObject(weatherObject: JSONObject): Weather {
        val weather = Weather()
        weather.date = Date(weatherObject.getLong("dt") * 1000)
        val main = weatherObject.getJSONObject("main")
        weather.temperature  = main.getDouble("temp")
        weather.pressure = main.getInt("pressure").toFloat()
        weather.humidity = main.getInt("humidity")
        val weatherJson = weatherObject.getJSONArray("weather").getJSONObject(0)
        val capitalizedDescription = capitalize(weatherJson.getString("description"))
        weather.description = capitalizedDescription
        weather.weatherId = weatherJson.getInt("id")
        setWind(weather, weatherObject.optJSONObject("wind"))
        setRain(weather, weatherObject.optJSONObject("rain"), weatherObject.optJSONObject("snow"))
        weather.lastUpdated  = Calendar.getInstance().timeInMillis
        return weather
    }

    @Throws(JSONException::class)
    private fun setWind(weather: Weather, windObject: JSONObject?) {
        if (windObject == null) {
            weather.wind = 0.0
            weather.windDirectionDegree = null
            return
        }
        weather.wind = windObject.getDouble("speed")
        if (windObject.has("deg")) {
            weather.windDirectionDegree = windObject.getDouble("deg")
        } else {
            Log.e("parseTodayJson", "No wind direction available")
            weather.windDirectionDegree = null
        }
    }

    @Throws(JSONException::class)
    private fun setRain(weather: Weather, rainObject: JSONObject?, snowObject: JSONObject?) {
        if (rainObject != null) {
            weather.rain = getRain(rainObject)
            return
        }
        if (snowObject != null) {
            weather.rain = getRain(snowObject)
            return
        }
        weather.rain = 0.0
    }

    @Throws(JSONException::class)
    private fun getRain(rainObject: JSONObject): Double {
        if (rainObject.has("3h")) {
            return rainObject.getDouble("3h")
        }
        return if (rainObject.has("1h")) {
            rainObject.getDouble("1h")
        } else 0.0
    }

    @Throws(JSONException::class)
    private fun setSunsetAndSunrise(weather: Weather, systemObject: JSONObject) {
        if (systemObject.has("sunrise") && systemObject.has("sunset")) {
            weather.setSunrise(systemObject.getString("sunrise"))
            weather.setSunset(systemObject.getString("sunset"))
        }
    }

    private fun setCityAndCountry(
        weather: Weather,
        cityId: Int,
        cityName: String,
        country: String
    ) {
        weather.cityId = cityId
        weather.city = cityName
        weather.country = country
    }

    @Throws(JSONException::class)
    private fun setCoordinates(weather: Weather, coordinatesObject: JSONObject) {
        weather.lat = coordinatesObject.getDouble("lat")
        weather.lon = coordinatesObject.getDouble("lon")
    }

    private fun capitalize(string: String): String {
        return if (string.isEmpty()) {
            string
        } else string.substring(0, 1).uppercase(Locale.getDefault()) + string.substring(1)
    }
}

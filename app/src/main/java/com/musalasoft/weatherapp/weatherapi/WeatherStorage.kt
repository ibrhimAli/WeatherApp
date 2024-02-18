package com.musalasoft.weatherapp.weatherapi

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.musalasoft.weatherapp.constant.Constants
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.weatherapi.owm.OpenWeatherMapJsonParser
import org.json.JSONException

class WeatherStorage(context: Context?) {
    protected var sharedPreferences: SharedPreferences

    init {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var cityId: Int
        get() {
            val cityIdString = sharedPreferences.getString("cityId", Constants.DEFAULT_CITY_ID)
            return cityIdString!!.toInt()
        }
        set(cityId) {
            val cityIdString = cityId.toString()
            sharedPreferences.edit().putString("cityId", cityIdString).apply()
        }
    var lastToday: Any?
        get(){
            val lastToday = sharedPreferences.getString("lastToday", null) ?: return null
            return try  {
                OpenWeatherMapJsonParser.convertJsonToWeather(lastToday)
            } catch (e: JSONException) {
                Log.e("WeatherStorage", "Could not parse today JSON", e)
                e.printStackTrace()
                null
            }
        }
        set(lastToday) {
            sharedPreferences.edit().putString("lastToday", lastToday as String).apply()
        }

    val lastLongTerm: List<Weather>?
        get() {
            val lastLongTerm = sharedPreferences.getString("lastLongterm", null) ?: return null
            return try {
                OpenWeatherMapJsonParser.convertJsonToWeatherList(lastLongTerm)
            } catch (e: JSONException) {
                Log.e("WeatherStorage", "Could not parse long term JSON", e)
                e.printStackTrace()
                null
            }
        }

    fun setLastLongTerm(lastLongTerm: String?) {
        sharedPreferences.edit().putString("lastLongterm", lastLongTerm).apply()
    }

    val lastUviToday: Double?
        get() {
            val lastUviToday = sharedPreferences.getString("lastUVIToday", null) ?: return null
            return try {
                OpenWeatherMapJsonParser.convertJsonToUVIndex(lastUviToday)
            } catch (e: JSONException) {
                Log.e("WeatherStorage", "Could not parse UV index JSON", e)
                e.printStackTrace()
                null
            }
        }

    fun setLastUviToday(lastUviToday: String?) {
        sharedPreferences.edit().putString("lastUVIToday", lastUviToday).apply()
    }

    var latitude: Double? = 0.0
        get() = if (sharedPreferences.contains("latitude")) {
            sharedPreferences.getFloat("latitude", 0f).toDouble()
        } else {
            null
        }

    fun getLatitude(defaultValue: Double): Double {
        val latitude = latitude
        return latitude ?: defaultValue
    }

    fun setLatitude(latitude: Double) {
        sharedPreferences.edit().putFloat("latitude", latitude.toFloat()).apply()
    }

    var longitude: Double? = 0.0
        get() {
            return if (sharedPreferences.contains("longitude")) {
                sharedPreferences.getFloat("longitude", 0f).toDouble()
            } else {
                null
            }
        }

    fun getLongitude(defaultValue: Double): Double {
        val longitude = longitude
        return longitude ?: defaultValue
    }

    fun setLongitude(longitude: Double) {
        sharedPreferences.edit().putFloat("longitude", longitude.toFloat()).apply()
    }
}

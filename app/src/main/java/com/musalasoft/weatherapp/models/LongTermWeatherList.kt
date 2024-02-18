package com.musalasoft.weatherapp.models

import java.util.Calendar

class LongTermWeatherList {
    private val longTermWeatherList: MutableList<Weather> = ArrayList<Weather>()
    val today: List<Weather>
        get() {
            val todayList: MutableList<Weather> = ArrayList<Weather>()
            val tomorrowCalendar = tomorrowCalendar
            for (weather in longTermWeatherList) {
                val weatherCalendar = getWeatherCalendar(weather)
                if (weatherCalendar.compareTo(tomorrowCalendar) < 0) {
                    todayList.add(weather)
                }
            }
            return todayList
        }
    val tomorrow: List<Weather>
        get() {
            val tomorrowList: MutableList<Weather> = ArrayList<Weather>()
            val tomorrowCalendar = tomorrowCalendar
            val laterCalendar = laterCalendar
            for (weather in longTermWeatherList) {
                val weatherCalendar = getWeatherCalendar(weather)
                if (weatherCalendar.compareTo(tomorrowCalendar) >= 0 && weatherCalendar.compareTo(
                        laterCalendar
                    ) < 0
                ) {
                    tomorrowList.add(weather)
                }
            }
            return tomorrowList
        }
    val later: List<Weather>
        get() {
            val laterList: MutableList<Weather> = ArrayList<Weather>()
            val laterCalendar = laterCalendar
            for (weather in longTermWeatherList) {
                val weatherCalendar = getWeatherCalendar(weather)
                if (weatherCalendar.compareTo(laterCalendar) >= 0) {
                    laterList.add(weather)
                }
            }
            return laterList
        }

    fun addAll(longTermWeather: List<Weather>?) {
        longTermWeatherList.addAll(longTermWeather!!)
    }

    fun clear() {
        longTermWeatherList.clear()
    }

    private val todayCalendar: Calendar
        private get() {
            val todayCalendar = Calendar.getInstance()
            todayCalendar[Calendar.HOUR_OF_DAY] = 0
            todayCalendar[Calendar.MINUTE] = 0
            todayCalendar[Calendar.SECOND] = 0
            todayCalendar[Calendar.MILLISECOND] = 0
            return todayCalendar
        }
    private val tomorrowCalendar: Calendar
        private get() {
            val tomorrowCalendar = todayCalendar
            tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1)
            return tomorrowCalendar
        }
    private val laterCalendar: Calendar
        private get() {
            val laterCalendar = todayCalendar
            laterCalendar.add(Calendar.DAY_OF_YEAR, 2)
            return laterCalendar
        }

    private fun getWeatherCalendar(weather: Weather): Calendar {
        val weatherCalendar = Calendar.getInstance()
        weatherCalendar.timeInMillis = weather.date!!.time
        return weatherCalendar
    }
}

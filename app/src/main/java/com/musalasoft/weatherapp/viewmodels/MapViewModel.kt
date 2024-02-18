package com.musalasoft.weatherapp.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.musalasoft.weatherapp.constant.Constants

class MapViewModel : ViewModel() {
    var sharedPreferences: SharedPreferences? = null
    var apiKey: String? = null
    var mapLat: Double = Constants.DEFAULT_LAT
    var mapLon: Double = Constants.DEFAULT_LON
    var mapZoom: Int = Constants.DEFAULT_ZOOM_LEVEL
    var tabPosition = 0
}

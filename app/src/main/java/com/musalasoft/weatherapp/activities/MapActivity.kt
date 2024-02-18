package com.musalasoft.weatherapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.utils.UI
import com.musalasoft.weatherapp.viewmodels.MapViewModel
import com.musalasoft.weatherapp.weatherapi.WeatherStorage

class MapActivity : BaseActivity() {
    private var webView: WebView? = null
    private lateinit var mapViewModel: MapViewModel
    private lateinit var weatherStorage: WeatherStorage
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        setTheme(UI.getTheme(prefs.getString("theme", "fresh")).also { theme = it })
        mapViewModel = ViewModelProvider(this).get(MapViewModel::class.java)
        weatherStorage = WeatherStorage(this)
        if (savedInstanceState == null) {
            mapViewModel.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            mapViewModel.mapLat = weatherStorage.getLatitude(0.0)
            mapViewModel.mapLon = weatherStorage.getLongitude(0.0)
            mapViewModel.apiKey = mapViewModel.sharedPreferences!!.getString(
                "apiKey",
                getResources().getString(R.string.apiKey)
            )
        }
        webView = findViewById(R.id.webView)
        webView!!.settings.javaScriptEnabled = true
        webView!!.loadUrl(
            (((("file:///android_asset/map.html?lat=" + mapViewModel.mapLat).toString() + "&lon="
                    + mapViewModel.mapLon).toString() + "&appid=" + mapViewModel.apiKey
                    ).toString() + "&zoom=" + mapViewModel.mapZoom).toString() + "&displayPin=true"
        )
        webView!!.addJavascriptInterface(HybridInterface(), "NativeInterface")
        webView!!.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (savedInstanceState != null) {
                    setMapState(mapViewModel.tabPosition)
                }
            }
        }
        val bottomBar: BottomNavigationView = findViewById(R.id.navigationBar)
        bottomBar.setOnNavigationItemSelectedListener { item ->
            val i = item.itemId
            setMapState(i)
            mapViewModel.tabPosition = i
            true
        }
    }

    private fun setMapState(item: Int) {
        when (item) {
            R.id.map_clouds -> webView!!.loadUrl(
                ("javascript:map.removeLayer(rainLayer);map.removeLayer(windLayer);map.removeLayer(tempLayer);"
                        + "map.addLayer(cloudsLayer);")
            )

            R.id.map_rain -> webView!!.loadUrl(
                ("javascript:map.removeLayer(cloudsLayer);map.removeLayer(windLayer);map.removeLayer(tempLayer);"
                        + "map.addLayer(rainLayer);")
            )

            R.id.map_wind -> webView!!.loadUrl(
                ("javascript:map.removeLayer(cloudsLayer);map.removeLayer(rainLayer);map.removeLayer(tempLayer);"
                        + "map.addLayer(windLayer);")
            )

            R.id.map_temperature -> webView!!.loadUrl(
                ("javascript:map.removeLayer(cloudsLayer);map.removeLayer(windLayer);map.removeLayer(rainLayer);"
                        + "map.addLayer(tempLayer);")
            )

            else -> Log.w("MapActivity", "Layer not configured")
        }
    }

    private inner class HybridInterface {
        @JavascriptInterface
        fun transferLatLon(lat: Double, lon: Double) {
            mapViewModel.mapLat = lat
            mapViewModel.mapLon = lon
        }

        @JavascriptInterface
        fun transferZoom(level: Int) {
            mapViewModel.mapZoom = level
        }
    }
}

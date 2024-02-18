package com.musalasoft.weatherapp.activities

import android.Manifest
import com.musalasoft.weatherapp.R
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.text.InputType
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.musalasoft.weatherapp.receiver.AlarmReceiver
import com.musalasoft.weatherapp.constant.Constants
import com.musalasoft.weatherapp.adapters.ViewPagerAdapter
import com.musalasoft.weatherapp.adapters.WeatherRecyclerAdapter
import com.musalasoft.weatherapp.fragments.AboutDialogFragment
import com.musalasoft.weatherapp.fragments.AmbiguousLocationDialogFragment
import com.musalasoft.weatherapp.fragments.RecyclerViewFragment
import com.musalasoft.weatherapp.models.LongTermWeatherList
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.tasks.GenericRequestTask
import com.musalasoft.weatherapp.tasks.ParseResult
import com.musalasoft.weatherapp.tasks.TaskOutput
import com.musalasoft.weatherapp.utils.Formatting
import com.musalasoft.weatherapp.utils.TimeUtils
import com.musalasoft.weatherapp.utils.UI
import com.musalasoft.weatherapp.utils.UnitConvertor
import com.musalasoft.weatherapp.weatherapi.WeatherStorage
import com.musalasoft.weatherapp.weatherapi.owm.OpenWeatherMapJsonParser
import com.musalasoft.weatherapp.widgets.AbstractWidgetProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.DecimalFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

class MainActivity : BaseActivity(), LocationListener {
    private var todayWeather: Weather = Weather()
    private lateinit var todayTemperature: TextView
    private lateinit var todayDescription: TextView
    private lateinit var todayWind: TextView
    private lateinit var todayPressure: TextView
    private lateinit var todayHumidity: TextView
    private lateinit var todaySunrise: TextView
    private lateinit var todaySunset: TextView
    private lateinit var todayUvIndex: TextView
    private lateinit var lastUpdate: TextView
    private lateinit var todayIcon: TextView
    private lateinit var tapGraph: TextView
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var appView: View
    private var locationManager: LocationManager? = null
    private lateinit var progressDialog: ProgressDialog
    override var theme: Int? = 0
    private var widgetTransparent = false
    private var destroyed = false
    private var firstRun = false
    private val longTermWeatherList: LongTermWeatherList = LongTermWeatherList()
    var recentCityId: Int? = null
    private lateinit var formatting: Formatting
    private lateinit var prefs: SharedPreferences
    private lateinit var linearLayoutTapForGraphs: LinearLayout
    private lateinit var weatherStorage: WeatherStorage
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize the associated SharedPreferences file with default values
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        firstRun = prefs.getBoolean("firstRun", true)
        widgetTransparent = prefs.getBoolean("transparentWidget", false)
        setTheme(UI.getTheme(prefs.getString("theme", "fresh")).also { theme = it })
        val darkTheme: Boolean = super.darkTheme
        val blackTheme: Boolean = super.blackTheme
        formatting = Formatting(this)

        // Initiate activity
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        appView = findViewById(R.id.viewApp)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        val appBarLayout: AppBarLayout = findViewById(R.id.appBarLayout)
        progressDialog = ProgressDialog(this@MainActivity)

        // Load toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (darkTheme) {
            toolbar.popupTheme = R.style.AppTheme_PopupOverlay_Dark
        } else if (blackTheme) {
            toolbar.popupTheme = R.style.AppTheme_PopupOverlay_Black
        }

        // Initialize textboxes
        todayTemperature = findViewById(R.id.todayTemperature)
        todayDescription = findViewById(R.id.todayDescription)
        todayWind = findViewById(R.id.todayWind)
        todayPressure = findViewById(R.id.todayPressure)
        todayHumidity = findViewById(R.id.todayHumidity)
        todaySunrise = findViewById(R.id.todaySunrise)
        todaySunset = findViewById(R.id.todaySunset)
        todayUvIndex = findViewById(R.id.todayUvIndex)
        lastUpdate = findViewById(R.id.lastUpdate)
        todayIcon = findViewById(R.id.todayIcon)
        tapGraph = findViewById(R.id.tapGraph)
        linearLayoutTapForGraphs = findViewById(R.id.linearLayout_tap_for_graphs)
        val weatherFont = Typeface.createFromAsset(this.assets, "fonts/weather.ttf")
        todayIcon.typeface = weatherFont

        // Initialize viewPager
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabs)
        destroyed = false
        initMappings()
        weatherStorage = WeatherStorage(this)

        // Preload data from cache
        preloadWeather()
        updateLastUpdateTime()

        // Set autoupdater
        AlarmReceiver.setRecurringAlarm(this)
        swipeRefreshLayout.setOnRefreshListener {
            refreshWeather()
            swipeRefreshLayout.isRefreshing = false
        }
        appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset -> // Only allow pull to refresh when scrolled to top
            swipeRefreshLayout.setEnabled(verticalOffset == 0)
        }

        // load weather for current device location once open the app
        cityByLocation
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        val bundle = intent.extras
        if (bundle != null && bundle.getBoolean(SHOULD_REFRESH_FLAG)) {
            refreshWeather()
        }
    }

    fun getAdapter(id: Int): WeatherRecyclerAdapter {
        val weatherRecyclerAdapter: WeatherRecyclerAdapter = when (id) {
            0 -> {
                WeatherRecyclerAdapter(longTermWeatherList.today)
            }

            1 -> {
                WeatherRecyclerAdapter(longTermWeatherList.tomorrow)
            }

            else -> {
                WeatherRecyclerAdapter(longTermWeatherList.later)
            }
        }
        return weatherRecyclerAdapter
    }

    override fun onStart() {
        super.onStart()
        preloadWeather()
    }

    override fun onResume() {
        super.onResume()
        if (UI.getTheme(prefs.getString("theme", "fresh")) !== theme ||
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("transparentWidget", false) != widgetTransparent
        ) {
            // Restart activity to apply theme
            overridePendingTransition(0, 0)
            prefs.edit().putBoolean("firstRun", true).apply()
            finish()
            overridePendingTransition(0, 0)
            startActivity(intent)
        } else if (shouldUpdate() && isNetworkAvailable) {
            getTodayWeather()
            longTermWeather
            todayUVIndex
        }
        if (firstRun) {
            tapGraph.text = getString(R.string.tap_for_graphs)
            prefs.edit().putBoolean("firstRun", false).apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyed = true
        if (locationManager != null) {
            try {
                locationManager!!.removeUpdates(this@MainActivity)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun preloadWeather() {
        val lastToday: Weather? = weatherStorage.lastToday as Weather?
        if (lastToday != null) {
            todayWeather = lastToday
            updateTodayWeatherUI()
            updateLastUpdateTime()
        }
        val lastUviToday: Double? = weatherStorage.lastUviToday
        if (lastUviToday != null) {
            todayWeather.uvIndex = lastUviToday
            updateUVIndexUI()
        }
        val lastLongTerm: List<Weather>? = weatherStorage.lastLongTerm
        if (!lastLongTerm.isNullOrEmpty()) {
            longTermWeatherList.clear()
            longTermWeatherList.addAll(lastLongTerm)
            updateLongTermWeatherUI()
        }
    }

    private val todayUVIndex: Unit
        private get() {
            val latitude: Double = weatherStorage.getLatitude(Constants.DEFAULT_LAT)
            val longitude: Double = weatherStorage.getLongitude(Constants.DEFAULT_LON)
            TodayUVITask(this, this, progressDialog).execute(
                "coords",
                latitude.toString(),
                longitude.toString()
            )
        }

    private fun getTodayWeather() {
        TodayWeatherTask(this, this, progressDialog).execute()
    }

    private val longTermWeather: Unit
        get() {
            LongTermWeatherTask(this, this, progressDialog).execute()
        }

    private fun searchCities() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.maxLines = 1
        input.isSingleLine = true
        val inputLayout = TextInputLayout(this)
        inputLayout.setPadding(32, 0, 32, 0)
        inputLayout.addView(input)
        val alert = AlertDialog.Builder(this)
        alert.setTitle(this.getString(R.string.search_title))
        alert.setView(inputLayout)
        alert.setPositiveButton(R.string.dialog_ok
        ) { dialog, whichButton ->
            val result = input.text.toString().trim { it <= ' ' }
            if (result.isNotEmpty()) {
                FindCitiesByNameTask(
                    applicationContext,
                    this@MainActivity, progressDialog
                ).execute("city", result)
            }
        }
        alert.setNegativeButton(R.string.dialog_cancel,
            { dialog, whichButton ->
                // Cancelled
            })
        alert.show()
    }

    private fun saveLocation(cityId: Int) {
        recentCityId = weatherStorage.cityId
        weatherStorage.cityId  = cityId

//        if (!recentCityId.equals(result)) {
//            // New location, update weather
//            getTodayWeather();
//            getLongTermWeather();
//            getTodayUVIndex();
//        }
    }

    private fun aboutDialog() {
        AboutDialogFragment().show(supportFragmentManager, null)
    }

    private fun parseTodayJson(result: String): ParseResult {
        try {
            val weatherUvIndex: Double = todayWeather.uvIndex
            todayWeather = OpenWeatherMapJsonParser.convertJsonToWeather(result)
            todayWeather.uvIndex = weatherUvIndex
            weatherStorage.lastToday = result
            weatherStorage.setLatitude(todayWeather.lat)
            weatherStorage.setLongitude(todayWeather.lon)
        } catch (e: JSONException) {
            Log.e("JSONException Data", result)
            e.printStackTrace()
            return ParseResult.JSON_EXCEPTION
        }
        return ParseResult.OK
    }

    private fun parseTodayUVIJson(result: String): ParseResult {
        try {
            val uvi: Double = OpenWeatherMapJsonParser.convertJsonToUVIndex(result)
            todayWeather.uvIndex = uvi
            weatherStorage.setLastUviToday(result)
        } catch (e: JSONException) {
            Log.e("JSONException Data", result)
            e.printStackTrace()
            return ParseResult.JSON_EXCEPTION
        }
        return ParseResult.OK
    }

    private fun updateTodayWeatherUI() {
        val city: String? = todayWeather.city
        val country: String? = todayWeather.country
        val timeFormat = DateFormat.getTimeFormat(applicationContext)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.title = city + if (country!!.isEmpty()) "" else ", $country"
        val sp = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

        // Temperature
        var temperature: Float =
            UnitConvertor.convertTemperature(todayWeather.temperature.toFloat(), sp)
        if (sp.getBoolean("temperatureInteger", false)) {
            temperature = Math.round(temperature).toFloat()
        }

        // Rain
        val rainString: String = UnitConvertor.getRainString(
            todayWeather.rain,
            todayWeather.chanceOfPrecipitation,
            sp
        )

        // Wind
        val wind: Double = UnitConvertor.convertWind(todayWeather.wind, sp)

        // Pressure
        val pressure: Double = UnitConvertor.convertPressure(todayWeather.pressure, sp).toDouble()
        todayTemperature.text =
            DecimalFormat("0.#").format(temperature.toDouble()) + " " + sp.getString("unit", "°C")
        todayDescription.text = (todayWeather.description!!.substring(0, 1).uppercase(Locale.getDefault()) +
                todayWeather.description!!.substring(1)) + rainString
        if ((sp.getString("speedUnit", "m/s") == "bft")) {
            todayWind.text = ((getString(R.string.wind) + ": " +
                    UnitConvertor.getBeaufortName(wind.toInt(), this)) +
                    (if (todayWeather.isWindDirectionAvailable) " " + getWindDirectionString(
                        sp,
                        this,
                        todayWeather
                    ) else ""))
        } else {
            todayWind.text = (getString(R.string.wind) + ": " + DecimalFormat("0.0").format(wind) + " " +
                    localize(sp, "speedUnit", "m/s") +
                    (if (todayWeather.isWindDirectionAvailable) " " + getWindDirectionString(
                        sp,
                        this,
                        todayWeather
                    ) else ""))
        }
        todayPressure.text = (getString(R.string.pressure) + ": " + DecimalFormat("0.0").format(pressure) + " " +
                localize(sp, "pressureUnit", "hPa"))
        todayHumidity.text = (getString(R.string.humidity) + ": " + todayWeather.humidity) + " %"
        todaySunrise.text = getString(R.string.sunrise) + ": " + timeFormat.format(todayWeather.sunrise)
        todaySunset.text = getString(R.string.sunset) + ": " + timeFormat.format(todayWeather.sunset)
        todayIcon.text = formatting.getWeatherIcon(
            todayWeather.weatherId,
            TimeUtils.isDayTime(todayWeather, Calendar.getInstance())
        )
        linearLayoutTapForGraphs.setOnClickListener {
            val intent = Intent(
                this@MainActivity,
                GraphActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun updateUVIndexUI() {
        // UV Index
        val uvIndex: Double = todayWeather.uvIndex
        todayUvIndex.text = (getString(R.string.uvindex) + ": " + uvIndex + " (" + UnitConvertor.convertUvIndexToRiskLevel(
            uvIndex,
            this
        )) + ")"
    }

    fun parseLongTermJson(result: String?): ParseResult {
        try {
            val weatherList: List<Weather> =
                OpenWeatherMapJsonParser.convertJsonToWeatherList(result)
            weatherStorage.setLastLongTerm(result)
            longTermWeatherList.clear()
            longTermWeatherList.addAll(weatherList)
        } catch (e: JSONException) {
            Log.e("JSONException Data", (result)!!)
            e.printStackTrace()
            return ParseResult.JSON_EXCEPTION
        }
        return ParseResult.OK
    }

    private fun updateLongTermWeatherUI() {
        if (destroyed) {
            return
        }
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        val bundleToday = Bundle()
        bundleToday.putInt("day", 0)
        val recyclerViewFragmentToday = RecyclerViewFragment()
        recyclerViewFragmentToday.setArguments(bundleToday)
        viewPagerAdapter.addFragment(recyclerViewFragmentToday, getString(R.string.today))
        val bundleTomorrow = Bundle()
        bundleTomorrow.putInt("day", 1)
        val recyclerViewFragmentTomorrow = RecyclerViewFragment()
        recyclerViewFragmentTomorrow.setArguments(bundleTomorrow)
        viewPagerAdapter.addFragment(recyclerViewFragmentTomorrow, getString(R.string.tomorrow))
        val bundle = Bundle()
        bundle.putInt("day", 2)
        val recyclerViewFragment = RecyclerViewFragment()
        recyclerViewFragment.setArguments(bundle)
        viewPagerAdapter.addFragment(recyclerViewFragment, getString(R.string.later))
        var currentPage = viewPager.currentItem
        viewPagerAdapter.notifyDataSetChanged()
        viewPager.setAdapter(viewPagerAdapter)
        tabLayout.setupWithViewPager(viewPager)
        if (currentPage == 0 && longTermWeatherList.today.isEmpty()) {
            currentPage = 1
        }
        viewPager.setCurrentItem(currentPage, false)
    }

    private val isNetworkAvailable: Boolean
        get() {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

    private fun shouldUpdate(): Boolean {
        val lastUpdate =
            PreferenceManager.getDefaultSharedPreferences(this).getLong("lastUpdate", -1)
        val cityChanged =
            PreferenceManager.getDefaultSharedPreferences(this).getBoolean("cityChanged", false)
        // Update if never checked or last update is longer ago than specified threshold
        return cityChanged || (lastUpdate < 0) || ((Calendar.getInstance().timeInMillis - lastUpdate) > NO_UPDATE_REQUIRED_THRESHOLD)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_refresh) {
            refreshWeather()
            return true
        }
        if (id == R.id.action_map) {
            val intent = Intent(
                this@MainActivity,
                MapActivity::class.java
            )
            startActivity(intent)
        }
        if (id == R.id.action_graphs) {
            val intent = Intent(
                this@MainActivity,
                GraphActivity::class.java
            )
            startActivity(intent)
        }
        if (id == R.id.action_search) {
            searchCities()
            return true
        }
        if (id == R.id.action_location) {
            cityByLocation
            return true
        }
        if (id == R.id.action_settings) {
            val intent: Intent = Intent(
                this@MainActivity,
                SettingsActivity::class.java
            )
            startActivity(intent)
        }
        if (id == R.id.action_about) {
            aboutDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun refreshWeather() {
        if (isNetworkAvailable) {
            getTodayWeather()
            longTermWeather
            todayUVIndex
        } else {
            Snackbar.make(
                appView,
                getString(R.string.msg_connection_not_available),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun localize(
        sp: SharedPreferences,
        preferenceKey: String,
        defaultValueKey: String
    ): String? {
        return localize(sp, this, preferenceKey, defaultValueKey)
    }

    val cityByLocation: Unit
        get() {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    showLocationSettingsDialog()
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION
                    )
                }
            } else if (locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                progressDialog = ProgressDialog(this)
                progressDialog.setMessage(getString(R.string.getting_location))
                progressDialog.setCancelable(false)
                progressDialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE, getString(R.string.dialog_cancel)
                ) { dialogInterface, i ->
                    try {
                        locationManager!!.removeUpdates(this@MainActivity)
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }
                progressDialog.show()
                if (locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0,
                        0f,
                        this
                    )
                }
                if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0f,
                        this
                    )
                }
            } else {
                showLocationSettingsDialog()
            }
        }

    private fun showLocationSettingsDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(R.string.location_settings)
        alertDialog.setMessage(R.string.location_settings_message)
        alertDialog.setPositiveButton(R.string.location_settings_button,
            { dialog, which ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            })
        alertDialog.setNegativeButton(R.string.dialog_cancel,
            { dialog, which -> dialog.cancel() })
        alertDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cityByLocation
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        progressDialog.hide()
        try {
            locationManager!!.removeUpdates(this)
        } catch (e: SecurityException) {
            Log.e(
                "LocationManager",
                "Error while trying to stop listening for location updates. This is probably a permissions issue",
                e
            )
        }
        Log.i(
            "LOCATION (" + location.provider!!.uppercase(Locale.getDefault()) + ")",
            location.latitude.toString() + ", " + location.longitude
        )
        val latitude = location.latitude
        val longitude = location.longitude
        ProvideCityNameTask(this, this, progressDialog).execute(
            "coords",
            latitude.toString(),
            longitude.toString()
        )
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    inner class TodayWeatherTask(
        context: Context?,
        activity: MainActivity?,
        progressDialog: ProgressDialog?
    ) :
        GenericRequestTask(context!!, activity!!, progressDialog!!) {
        override fun onPreExecute() {
            loading = 0
            super.onPreExecute()
        }

        override fun onPostExecute(output: TaskOutput) {
            super.onPostExecute(output)
            // Update widgets
            AbstractWidgetProvider.updateWidgets(context)
        }

        override fun parseResponse(response: String?): ParseResult {
            return parseTodayJson(response!!)
        }

        override val aPIName: String
             get() = "weather"

        override fun updateMainUI() {
            updateTodayWeatherUI()
            updateLastUpdateTime()
        }
    }

    internal inner class LongTermWeatherTask(
        context: Context?,
        activity: MainActivity?,
        progressDialog: ProgressDialog?
    ) :
        GenericRequestTask(context!!, activity!!, progressDialog!!) {
        override fun parseResponse(response: String?): ParseResult {
            return parseLongTermJson(response)
        }

        override val aPIName: String
            protected get() = "forecast"

        override fun updateMainUI() {
            updateLongTermWeatherUI()
        }
    }

    internal inner class FindCitiesByNameTask(
        context: Context?,
        activity: MainActivity?,
        progressDialog: ProgressDialog?
    ) :
        GenericRequestTask(context!!, activity!!, progressDialog!!) {
        override fun onPreExecute() { /*Nothing*/
        }

        override fun parseResponse(response: String?): ParseResult {
            try {
                val reader = JSONObject(response)
                val count = reader.optInt("count")
                if (count == 0) {
                    Log.e("Geolocation", "No city found")
                    return ParseResult.CITY_NOT_FOUND
                }

//                saveLocation(reader.getString("id"));
                val cityList = reader.getJSONArray("list")
                if (cityList.length() > 1) {
                    launchLocationPickerDialog(cityList)
                } else {
                    saveLocation(cityList.getJSONObject(0).getInt("id"))
                }
            } catch (e: JSONException) {
                Log.e("JSONException Data", (response)!!)
                e.printStackTrace()
                return ParseResult.JSON_EXCEPTION
            }
            return ParseResult.OK
        }

        override val aPIName: String
            protected get() = "find"

        override fun onPostExecute(output: TaskOutput) {
            /* Handle possible errors only */
            handleTaskOutput(output)
            refreshWeather()
        }
    }

    private fun launchLocationPickerDialog(cityList: JSONArray) {
        val fragment = AmbiguousLocationDialogFragment()
        val bundle = Bundle()
        val fragmentTransaction: FragmentTransaction =
            supportFragmentManager.beginTransaction()
        bundle.putString("cityList", cityList.toString())
        fragment.setArguments(bundle)
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.add(android.R.id.content, fragment)
            .addToBackStack(null).commit()
    }

    internal inner class ProvideCityNameTask(
        context: Context?,
        activity: MainActivity?,
        progressDialog: ProgressDialog?
    ) :
        GenericRequestTask(context!!, activity!!, progressDialog!!) {
        override fun onPreExecute() { /*Nothing*/
        }

        override val aPIName: String
            protected get() = "weather"

        override fun parseResponse(response: String?): ParseResult {
            Log.i("RESULT", response.toString())
            try {
                val reader = JSONObject(response)
                val code = reader.optString("cod")
                if (("404" == code)) {
                    Log.e("Geolocation", "No city found")
                    return ParseResult.CITY_NOT_FOUND
                }
                saveLocation(reader.getInt("id"))
            } catch (e: JSONException) {
                Log.e("JSONException Data", response!!)
                e.printStackTrace()
                return ParseResult.JSON_EXCEPTION
            }
            return ParseResult.OK
        }

        override fun onPostExecute(output: TaskOutput) {
            /* Handle possible errors only */
            handleTaskOutput(output)
            refreshWeather()
        }
    }

    internal inner class TodayUVITask(
        context: Context?,
        activity: MainActivity?,
        progressDialog: ProgressDialog?
    ) :
        GenericRequestTask(context!!, activity!!, progressDialog!!) {
            override fun onPreExecute() {
            loading = 0
            super.onPreExecute()
        }

        override fun parseResponse(response: String?): ParseResult {
            return parseTodayUVIJson(response!!)
        }

        override val aPIName: String
            protected get() = "uvi"

        override fun updateMainUI() {
            updateUVIndexUI()
        }
    }

    private fun updateLastUpdateTime(
        timeInMillis: Long =
            PreferenceManager.getDefaultSharedPreferences(this).getLong("lastUpdate", -1)
    ) {
        if (timeInMillis < 0) {
            // No time
            lastUpdate.text = ""
        } else {
            lastUpdate.text = getString(
                R.string.last_update,
                formatTimeWithDayIfNotToday(this, timeInMillis)
            )
        }
    }

    companion object {
        val MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1
        val SHOULD_REFRESH_FLAG = "shouldRefresh"

        // Time in milliseconds; only reload weather if last update is longer ago than this value
        private val NO_UPDATE_REQUIRED_THRESHOLD = 300000
        private val speedUnits: MutableMap<String?, Int> = HashMap(3)
        private val pressUnits: MutableMap<String?, Int> = HashMap(3)
        private var mappingsInitialised = false
        fun getRainString(rainObj: JSONObject?): String {
            var rain = "0"
            if (rainObj != null) {
                rain = rainObj.optString("3h", "fail")
                if (("fail" == rain)) {
                    rain = rainObj.optString("1h", "0")
                }
            }
            return rain
        }

        fun initMappings() {
            if (mappingsInitialised) return
            mappingsInitialised = true
            speedUnits["m/s"] = R.string.speed_unit_mps
            speedUnits["kph"] = R.string.speed_unit_kph
            speedUnits["mph"] = R.string.speed_unit_mph
            speedUnits["kn"] = R.string.speed_unit_kn
            pressUnits["hPa"] = R.string.pressure_unit_hpa
            pressUnits["kPa"] = R.string.pressure_unit_kpa
            pressUnits["mm Hg"] = R.string.pressure_unit_mmhg
            pressUnits["in Hg"] = R.string.pressure_unit_inhg
        }

        fun localize(
            sp: SharedPreferences,
            context: Context,
            preferenceKey: String,
            defaultValueKey: String?
        ): String? {
            val preferenceValue = sp.getString(preferenceKey, defaultValueKey)
            var result = preferenceValue
            if (("speedUnit" == preferenceKey)) {
                if (speedUnits.containsKey(preferenceValue)) {
                    result = context.getString((speedUnits[preferenceValue])!!)
                }
            } else if (("pressureUnit" == preferenceKey)) {
                if (pressUnits.containsKey(preferenceValue)) {
                    result = context.getString((pressUnits[preferenceValue])!!)
                }
            }
            return result
        }

        fun getWindDirectionString(
            sp: SharedPreferences,
            context: Context?,
            weather: Weather
        ): String {
            try {
                if (weather.wind != 0.0) {
                    val pref = sp.getString("windDirectionFormat", null)
                    if (("arrow" == pref)) {
                        return weather.getWindDirection(8).getArrow(context!!)
                    } else if (("abbr" == pref)) {
                        return weather.windDirection.getLocalizedString(context!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

        fun saveLastUpdateTime(sp: SharedPreferences): Long {
            val now = Calendar.getInstance()
            val lastUpdate = now.timeInMillis
            sp.edit().putLong("lastUpdate", lastUpdate).commit()
            return lastUpdate
        }

        fun formatTimeWithDayIfNotToday(context: Context?, timeInMillis: Long): String {
            val now = Calendar.getInstance()
            val lastCheckedCal: Calendar = GregorianCalendar()
            lastCheckedCal.timeInMillis = timeInMillis
            val lastCheckedDate = Date(timeInMillis)
            val timeFormat = DateFormat.getTimeFormat(context).format(lastCheckedDate)
            return if (now.get(Calendar.YEAR) == lastCheckedCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == lastCheckedCal.get(Calendar.DAY_OF_YEAR)
            ) {
                // Same day, only show time
                timeFormat
            } else {
                DateFormat.getDateFormat(context).format(lastCheckedDate) + " " + timeFormat
            }
        }
    }
}

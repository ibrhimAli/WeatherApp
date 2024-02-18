package com.musalasoft.weatherapp.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.adapters.LocationsRecyclerAdapter
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.utils.UnitConvertor
import com.musalasoft.weatherapp.weatherapi.WeatherStorage
import org.json.JSONArray
import org.json.JSONException
import java.util.Locale

class AmbiguousLocationDialogFragment : DialogFragment(),
    LocationsRecyclerAdapter.ItemClickListener {
    private var recyclerAdapter: LocationsRecyclerAdapter? = null
    private var sharedPreferences: SharedPreferences? = null
    private var weatherStorage: WeatherStorage? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_ambiguous_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = arguments
        val toolbar = view.findViewById<Toolbar>(R.id.dialogToolbar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.locationsRecyclerView)
        val linearLayout = view.findViewById<LinearLayout>(R.id.locationsLinearLayout)
        toolbar.setTitle(getString(R.string.location_search_heading))
        toolbar.setNavigationIcon(R.drawable.ic_close_black_24dp)
        toolbar.setNavigationOnClickListener { close() }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        weatherStorage = WeatherStorage(activity)
        val theme = getTheme(sharedPreferences!!.getString("theme", "fresh"))
        val darkTheme = theme == R.style.AppTheme_NoActionBar_Dark ||
                theme == R.style.AppTheme_NoActionBar_Classic_Dark
        val blackTheme = theme == R.style.AppTheme_NoActionBar_Black ||
                theme == R.style.AppTheme_NoActionBar_Classic_Black
        if (darkTheme) {
            linearLayout.setBackgroundColor(Color.parseColor("#2f2f2f"))
        }
        if (blackTheme) {
            linearLayout.setBackgroundColor(Color.BLACK)
        }
        try {
            val cityListArray = JSONArray(bundle!!.getString("cityList"))
            val weatherArrayList: ArrayList<Weather> = ArrayList<Weather>()
            recyclerAdapter = LocationsRecyclerAdapter(
                view.context.applicationContext,
                weatherArrayList, darkTheme, blackTheme
            )
            recyclerAdapter!!.setClickListener(this@AmbiguousLocationDialogFragment)
            for (i in 0 until cityListArray.length()) {
                val cityObject = cityListArray.getJSONObject(i)
                val weatherObject = cityObject.getJSONArray("weather").getJSONObject(0)
                val mainObject = cityObject.getJSONObject("main")
                val coordObject = cityObject.getJSONObject("coord")
                val sysObject = cityObject.getJSONObject("sys")
                val city = cityObject.getString("name")
                val country = sysObject.getString("country")
                val cityId = cityObject.getInt("id")
                val description = weatherObject.getString("description")
                val weatherId = weatherObject.getInt("id")
                val temperature: Float = UnitConvertor.convertTemperature(
                    mainObject.getDouble("temp").toFloat(),
                    sharedPreferences!!
                )
                val lat = coordObject.getDouble("lat")
                val lon = coordObject.getDouble("lon")
                val weather = Weather()
                weather.city = city
                weather.cityId = cityId
                weather.country = country
                weather.weatherId = weatherId
                weather.description =
                    description.substring(0, 1)
                        .uppercase(Locale.getDefault()) + description.substring(1)

                weather.temperature  = temperature.toDouble()
                weather.lat = lat
                weather.lon = lon
                weatherArrayList.add(weather)
            }
            recyclerView.setLayoutManager(LinearLayoutManager(activity))
            recyclerView.setAdapter(recyclerAdapter)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun onItemClickListener(view: View?, position: Int) {
        val weather: Weather = recyclerAdapter!!.getItem(position)
        val intent = Intent(activity, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val bundle = Bundle()
        weatherStorage!!.cityId  = weather.cityId
        weatherStorage!!.latitude = weather.lat
        weatherStorage!!.longitude = weather.lon
        bundle.putBoolean(MainActivity.SHOULD_REFRESH_FLAG, true)
        intent.putExtras(bundle)
        startActivity(intent)
        close()
    }

    private fun getTheme(themePref: String?): Int {
        return when (themePref) {
            "dark" -> R.style.AppTheme_NoActionBar_Dark
            "black" -> R.style.AppTheme_NoActionBar_Black
            "classic" -> R.style.AppTheme_NoActionBar_Classic
            "classicdark" -> R.style.AppTheme_NoActionBar_Classic_Dark
            "classicblack" -> R.style.AppTheme_NoActionBar_Classic_Black
            else -> R.style.AppTheme_NoActionBar
        }
    }

    private fun close() {
        val activity = activity
        activity?.supportFragmentManager?.popBackStack()
    }
}

package com.musalasoft.weatherapp.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.utils.Formatting
import com.musalasoft.weatherapp.utils.TimeUtils
import java.text.DecimalFormat
import java.util.Calendar

class LocationsRecyclerAdapter(
    private val context: Context,
    weatherArrayList: ArrayList<Weather>,
    darkTheme: Boolean,
    blackTheme: Boolean
) :
    RecyclerView.Adapter<LocationsRecyclerAdapter.LocationsViewHolder>() {
    private val inflater: LayoutInflater
    private var itemClickListener: ItemClickListener? = null
    private val weatherArrayList: ArrayList<Weather>
    private val darkTheme: Boolean
    private val blackTheme: Boolean
    private val decimalZeroes: Boolean
    private val temperatureUnit: String?
    private val formatting: Formatting

    init {
        this.weatherArrayList = weatherArrayList
        this.darkTheme = darkTheme
        this.blackTheme = blackTheme
        inflater = LayoutInflater.from(context)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context
        )
        decimalZeroes = sharedPreferences.getBoolean("displayDecimalZeroes", false)
        temperatureUnit = sharedPreferences.getString("unit", "°C")
        formatting = Formatting(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationsViewHolder {
        return LocationsViewHolder(inflater.inflate(R.layout.list_location_row, parent, false))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onBindViewHolder(holder: LocationsViewHolder, position: Int) {
        val weatherFont = Typeface.createFromAsset(context.assets, "fonts/weather.ttf")
        val weather: Weather = weatherArrayList[position]
        holder.cityTextView.text =
            java.lang.String.format("%s, %s", weather.city, weather.country)
        holder.descriptionTextView.text = weather.description
        holder.iconTextView.text = formatting.getWeatherIcon(
            weather.weatherId,
            TimeUtils.isDayTime(weather, Calendar.getInstance())
        )
        holder.iconTextView.typeface = weatherFont
        if (decimalZeroes) {
            holder.temperatureTextView.text = DecimalFormat("0.0").format(weather.temperature) + " " + temperatureUnit
        } else {
            holder.temperatureTextView.text = DecimalFormat("#.#").format(weather.temperature) + " " + temperatureUnit
        }
        holder.webView.settings.javaScriptEnabled = true
        holder.webView.loadUrl((("file:///android_asset/map.html?lat=" + weather.lat).toString() + "&lon=" + weather.lon).toString() + "&zoom=" + 10 + "&appid=notneeded&displayPin=true")
        if (darkTheme || blackTheme) {
            holder.cityTextView.setTextColor(Color.WHITE)
            holder.temperatureTextView.setTextColor(Color.WHITE)
            holder.descriptionTextView.setTextColor(Color.WHITE)
            holder.iconTextView.setTextColor(Color.WHITE)
        }
        if (darkTheme) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#2e3c43"))
        }
        if (blackTheme) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#2f2f2f"))
        }
    }

    override fun getItemCount(): Int {
        return weatherArrayList.size
    }

    inner class LocationsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val cityTextView: TextView
        val temperatureTextView: TextView
        val descriptionTextView: TextView
        val iconTextView: TextView
        val webView: WebView
        val cardView: CardView

        init {
            cityTextView = itemView.findViewById<TextView>(R.id.rowCityTextView)
            temperatureTextView = itemView.findViewById<TextView>(R.id.rowTemperatureTextView)
            descriptionTextView = itemView.findViewById<TextView>(R.id.rowDescriptionTextView)
            iconTextView = itemView.findViewById<TextView>(R.id.rowIconTextView)
            webView = itemView.findViewById<WebView>(R.id.webView2)
            cardView = itemView.findViewById<CardView>(R.id.rowCardView)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (itemClickListener != null) {
                itemClickListener!!.onItemClickListener(view, getAdapterPosition())
            }
        }
    }

    fun getItem(position: Int): Weather {
        return weatherArrayList[position]
    }

    fun setClickListener(itemClickListener: ItemClickListener?) {
        this.itemClickListener = itemClickListener
    }

    interface ItemClickListener {
        fun onItemClickListener(view: View?, position: Int)
    }
}

package com.musalasoft.weatherapp.models

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.musalasoft.weatherapp.R

class WeatherViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var itemDate: TextView
    var itemTemperature: TextView
    var itemDescription: TextView
    var itemyWind: TextView
    var itemPressure: TextView
    var itemHumidity: TextView
    var itemIcon: TextView
    var lineView: View

    init {
        itemDate = view.findViewById<View>(R.id.itemDate) as TextView
        itemTemperature = view.findViewById<View>(R.id.itemTemperature) as TextView
        itemDescription = view.findViewById<View>(R.id.itemDescription) as TextView
        itemyWind = view.findViewById<View>(R.id.itemWind) as TextView
        itemPressure = view.findViewById<View>(R.id.itemPressure) as TextView
        itemHumidity = view.findViewById<View>(R.id.itemHumidity) as TextView
        itemIcon = view.findViewById<View>(R.id.itemIcon) as TextView
        lineView = view.findViewById<View>(R.id.lineView)
    }
}

package com.musalasoft.weatherapp.adapters

import android.content.Context
import android.graphics.Typeface
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.activities.MainActivity
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.models.WeatherViewHolder
import com.musalasoft.weatherapp.utils.Formatting
import com.musalasoft.weatherapp.utils.TimeUtils
import com.musalasoft.weatherapp.utils.UnitConvertor
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WeatherRecyclerAdapter(itemList: List<Weather>?) :
    RecyclerView.Adapter<WeatherViewHolder>() {
    private val itemList: List<Weather>?

    init {
        this.itemList = itemList
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): WeatherViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_row, viewGroup, false)
        return WeatherViewHolder(view)
    }

    override fun onBindViewHolder(customViewHolder: WeatherViewHolder, i: Int) {
        if (i < 0 || i >= itemList!!.size) return
        val context: Context = customViewHolder.itemView.context
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val weatherItem: Weather = itemList[i]

        // Temperature
        var temperature: Float =
            UnitConvertor.convertTemperature(weatherItem.temperature.toFloat(), sp)
        if (sp.getBoolean("temperatureInteger", false)) {
            temperature = Math.round(temperature).toFloat()
        }

        // Rain
        val rainString: String = UnitConvertor.getRainString(
            weatherItem.rain,
            weatherItem.chanceOfPrecipitation,
            sp
        )

        // Wind
        val wind: Double = UnitConvertor.convertWind(weatherItem.wind, sp)

        // Pressure
        val pressure: Double = UnitConvertor.convertPressure(weatherItem.pressure, sp).toDouble()
        val tz = TimeZone.getDefault()
        val defaultDateFormat = context.resources.getStringArray(R.array.dateFormatsValues)[0]
        var dateFormat = sp.getString("dateFormat", defaultDateFormat)
        if ("custom" == dateFormat) {
            dateFormat = sp.getString("dateFormatCustom", defaultDateFormat)
        }
        var dateString: String
        try {
            val resultFormat = SimpleDateFormat(dateFormat)
            resultFormat.timeZone = tz
            dateString = resultFormat.format(weatherItem.date!!)
        } catch (e: IllegalArgumentException) {
            dateString = context.resources.getString(R.string.error_dateFormat)
        }
        if (sp.getBoolean("differentiateDaysByTint", false)) {
            val now = Date()
            /* Unfortunately, the getColor() that takes a theme (the next commented line) is Android 6.0 only, so we have to do it manually
             * customViewHolder.itemView.setBackgroundColor(context.getResources().getColor(R.attr.colorTintedBackground, context.getTheme())); */
            val color: Int
            if (weatherItem.getNumDaysFrom(now) > 1) {
                val ta = context.obtainStyledAttributes(
                    intArrayOf(
                        R.attr.colorTintedBackground,
                        R.attr.colorBackground
                    )
                )
                color = if (weatherItem.getNumDaysFrom(now) % 2 == 1L) {
                    ta.getColor(0, context.resources.getColor(R.color.colorTintedBackground))
                } else {
                    /* We must explicitly set things back, because RecyclerView seems to reuse views and
                                 * without restoring back the "normal" color, just about everything gets tinted if we
                                 * scroll a couple of times! */
                    ta.getColor(1, context.resources.getColor(R.color.colorBackground))
                }
                ta.recycle()
                customViewHolder.itemView.setBackgroundColor(color)
            }
        }
        customViewHolder.itemDate.text = dateString
        if (sp.getBoolean("displayDecimalZeroes", false)) {
            customViewHolder.itemTemperature.text = DecimalFormat("0.0").format(temperature.toDouble()) + " " + sp.getString(
                "unit",
                "°C"
            )
        } else {
            customViewHolder.itemTemperature.text = DecimalFormat("#.#").format(temperature.toDouble()) + " " + sp.getString(
                "unit",
                "°C"
            )
        }
        customViewHolder.itemDescription.text = (weatherItem.description!!.substring(0, 1).uppercase(Locale.getDefault()) +
                weatherItem.description!!.substring(1)).toString() + rainString
        val weatherFont = Typeface.createFromAsset(context.assets, "fonts/weather.ttf")
        customViewHolder.itemIcon.typeface = weatherFont
        customViewHolder.itemIcon.text = getWeatherIcon(weatherItem, context)
        if ((sp.getString("speedUnit", "m/s") == "bft")) {
            customViewHolder.itemyWind.text = ((context.getString(R.string.wind) + ": " +
                    UnitConvertor.getBeaufortName(
                        wind.toInt(),
                        context
                    )).toString() + " " + MainActivity.getWindDirectionString(
                sp,
                context,
                weatherItem
            ))
        } else {
            customViewHolder.itemyWind.text = ((context.getString(R.string.wind) + ": " + DecimalFormat("0.0").format(wind) + " " +
                    MainActivity.localize(sp, context, "speedUnit", "m/s")
                    ).toString() + " " + MainActivity.getWindDirectionString(
                sp,
                context,
                weatherItem
            ))
        }
        customViewHolder.itemPressure.text = (context.getString(R.string.pressure) + ": " + DecimalFormat("0.0").format(pressure) + " " +
                MainActivity.localize(sp, context, "pressureUnit", "hPa"))
        customViewHolder.itemHumidity.text = (context.getString(R.string.humidity) + ": " + weatherItem.humidity).toString() + " %"
    }

    override fun getItemCount(): Int {
        return (itemList?.size ?: 0)
    }

    private fun getWeatherIcon(weather: Weather, context: Context): String {
        val formatting = Formatting(context)
        return formatting.getWeatherIcon(
            weather.weatherId,
            TimeUtils.isDayTime(weather, Calendar.getInstance())
        )
    }
}

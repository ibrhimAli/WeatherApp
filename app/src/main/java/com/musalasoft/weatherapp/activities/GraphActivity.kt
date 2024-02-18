package com.musalasoft.weatherapp.activities

import com.musalasoft.weatherapp.R
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import com.db.chart.Tools
import com.db.chart.model.BarSet
import com.db.chart.model.ChartSet
import com.db.chart.model.LineSet
import com.db.chart.view.BarChartView
import com.db.chart.view.ChartView
import com.db.chart.view.LineChartView
import com.google.android.material.snackbar.Snackbar
import com.musalasoft.weatherapp.models.Weather
import com.musalasoft.weatherapp.tasks.ParseResult
import com.musalasoft.weatherapp.utils.UI
import com.musalasoft.weatherapp.utils.UnitConvertor
import com.musalasoft.weatherapp.weatherapi.owm.OpenWeatherMapJsonParser
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class GraphActivity : BaseActivity() {
    private lateinit var sp: SharedPreferences
    override var theme: Int? = 0
    private val weatherList: ArrayList<Weather> = ArrayList<Weather>()
    private val gridPaint: Paint = object : Paint() {
        init {
            style = Style.STROKE
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
            strokeWidth = 1f
        }
    }
    private val dateFormat: SimpleDateFormat = object : SimpleDateFormat("E") {
        init {
            timeZone = TimeZone.getDefault()
        }
    }
    private var labelColor = "#000000"
    private var lineColor = "#333333"
    private var backgroundBarColor = "#000000"
    override var darkTheme = false
    private var numWeatherData = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
        val toolbar: Toolbar = findViewById(R.id.graph_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        setTheme(UI.getTheme(sp.getString("theme", "fresh")).also { theme = it })
        darkTheme =
            theme === R.style.AppTheme_NoActionBar_Dark || theme === R.style.AppTheme_NoActionBar_Black || theme === R.style.AppTheme_NoActionBar_Classic_Dark || theme === R.style.AppTheme_NoActionBar_Classic_Black
        val graphSwitch: Switch = findViewById(R.id.graph_switch)
        graphSwitch.isChecked = sp.getString("graphsMoreDays", "off") == "on"
        graphSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // show graphs for whole five-day forecast
                numWeatherData = weatherList.size
                sp.edit().putString("graphsMoreDays", "on").apply()
            } else {
                // show graphs for only two days
                numWeatherData = 2 * weatherList.size / 5
                sp.edit().putString("graphsMoreDays", "off").apply()
            }
            updateGraphs()
        }
        val temperatureTextView: TextView = findViewById(R.id.graph_temperature_textview)
        temperatureTextView.text =
            java.lang.String.format(
                "%s (%s)",
                getString(R.string.temperature),
                sp.getString("unit", "°C")
            )
        val rainTextView: TextView = findViewById(R.id.graph_rain_textview)
        rainTextView.text = java.lang.String.format(
            "%s (%s)",
            getString(R.string.rain),
            sp.getString("lengthUnit", "mm")
        )
        val windSpeedTextView: TextView = findViewById(R.id.graph_windspeed_textview)
        windSpeedTextView.text =
            java.lang.String.format(
                "%s (%s)",
                getString(R.string.wind_speed),
                sp.getString("speedUnit", "m/s")
            )
        val pressureTextView: TextView = findViewById(R.id.graph_pressure_textview)
        pressureTextView.text = java.lang.String.format(
            "%s (%s)",
            getString(R.string.pressure),
            sp.getString("pressureUnit", "hPa/mBar")
        )
        val humidityTextView: TextView = findViewById(R.id.graph_humidity_textview)
        humidityTextView.text =
            java.lang.String.format("%s (%s)", getString(R.string.humidity), "%")
        if (darkTheme) {
            toolbar.popupTheme = R.style.AppTheme_PopupOverlay_Dark
            labelColor = "#FFFFFF"
            lineColor = "#FAFAFA"
            backgroundBarColor = "#FFFFFF"
            temperatureTextView.setTextColor(Color.parseColor(labelColor))
            rainTextView.setTextColor(Color.parseColor(labelColor))
            windSpeedTextView.setTextColor(Color.parseColor(labelColor))
            pressureTextView.setTextColor(Color.parseColor(labelColor))
            humidityTextView.setTextColor(Color.parseColor(labelColor))
        }
        gridPaint.color = Color.parseColor(lineColor)
        val lastLongterm = sp.getString("lastLongterm", "")
        if (parseLongTermJson(lastLongterm) === ParseResult.OK) {
            numWeatherData = if (sp.getString("graphsMoreDays", "off") == "off") {
                2 * weatherList.size / 5
            } else {
                weatherList.size
            }
            updateGraphs()
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.msg_err_parsing_json,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun updateGraphs() {
        temperatureGraph()
        rainGraph()
        windSpeedGraph()
        pressureGraph()
        humidityGraph()
    }

    private fun temperatureGraph() {
        val lineChartView: LineChartView = findViewById(R.id.graph_temperature)
        var minTemp = 1000f
        var maxTemp = -1000f
        val dataset = LineSet()
        for (i in 0 until numWeatherData) {
            val temperature: Float =
                UnitConvertor.convertTemperature(weatherList[i].temperature.toFloat(), sp)
            minTemp =
                min(floor(temperature.toDouble()), minTemp.toDouble()).toFloat()
            maxTemp = max(ceil(temperature.toDouble()), maxTemp.toDouble()).toFloat()
            dataset.addPoint(getDateLabel(weatherList[i], i), temperature)
        }
        dataset.setSmooth(false)
        dataset.setColor(Color.parseColor("#FF5722"))
        dataset.setThickness(4F)
        val middle = Math.round(minTemp + (maxTemp - minTemp) / 2)
        val stepSize = max(
            1.0, ceil((abs((maxTemp - minTemp).toDouble()) / 4).toDouble())
                .toInt().toDouble()
        ).toInt()
        val min = middle - 2 * stepSize
        val max = middle + 2 * stepSize
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(dataset)
        lineChartView.addData(data)
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, 4, 1, gridPaint)
        lineChartView.setAxisBorderValues(min, max)
        lineChartView.setStep(stepSize)
        lineChartView.setLabelsColor(Color.parseColor(labelColor))
        lineChartView.setXAxis(false)
        lineChartView.setYAxis(false)
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        lineChartView.show()
        val backgroundChartView: BarChartView =
            getBackgroundBarChart(R.id.graph_temperature_background, min, max, false)
        backgroundChartView.show()
    }

    private fun rainGraph() {
        val barChartView: BarChartView = findViewById(R.id.graph_rain)
        var maxRain = 1f
        val dataset = BarSet()
        for (i in 0 until numWeatherData) {
            val rain: Float = UnitConvertor.convertRain(weatherList[i].rain.toFloat(), sp)
            maxRain = max(rain.toDouble(), maxRain.toDouble()).toFloat()
            dataset.addBar(getDateLabel(weatherList[i], i), rain)
        }
        dataset.setColor(Color.parseColor("#2196F3"))
        var stepSize = 1
        if (maxRain > 6) {
            maxRain = ceil((maxRain / 6).toDouble()).toFloat() * 6
            stepSize = ceil((maxRain / 6).toDouble()).toInt()
        } else {
            maxRain = ceil(maxRain.toDouble()).toFloat()
        }
        val max = maxRain.toInt()
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(dataset)
        barChartView.addData(data)
        barChartView.setGrid(ChartView.GridType.HORIZONTAL, max / stepSize, 1, gridPaint)
        barChartView.setAxisBorderValues(0, ceil(maxRain.toDouble()).toInt())
        barChartView.setStep(stepSize)
        barChartView.setLabelsColor(Color.parseColor(labelColor))
        barChartView.setXAxis(false)
        barChartView.setYAxis(false)
        barChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        barChartView.show()
        val backgroundChartView: BarChartView =
            getBackgroundBarChart(R.id.graph_rain_background, 0, max, true)
        backgroundChartView.show()
    }

    private fun windSpeedGraph() {
        val lineChartView: LineChartView = findViewById(R.id.graph_windspeed)
        var graphLineColor = "#efd214"
        var maxWindSpeed = 1f
        if (darkTheme) {
            graphLineColor = "#FFF600"
        }
        val dataset = LineSet()
        for (i in 0 until numWeatherData) {
            val windSpeed = UnitConvertor.convertWind(weatherList[i].wind, sp).toFloat()
            maxWindSpeed = max(windSpeed.toDouble(), maxWindSpeed.toDouble()).toFloat()
            dataset.addPoint(getDateLabel(weatherList[i], i), windSpeed)
        }
        dataset.setSmooth(false)
        dataset.setColor(Color.parseColor(graphLineColor))
        dataset.setThickness(4F)
        var stepSize = 1
        if (maxWindSpeed > 6) {
            maxWindSpeed = ceil((maxWindSpeed / 6).toDouble()).toFloat() * 6
            stepSize = ceil((maxWindSpeed / 6).toDouble()).toInt()
        } else {
            maxWindSpeed = ceil(maxWindSpeed.toDouble()).toFloat()
        }
        val max = maxWindSpeed.toInt()
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(dataset)
        lineChartView.addData(data)
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, max / stepSize, 1, gridPaint)
        lineChartView.setAxisBorderValues(0, maxWindSpeed.toInt())
        lineChartView.setStep(stepSize)
        lineChartView.setLabelsColor(Color.parseColor(labelColor))
        lineChartView.setXAxis(false)
        lineChartView.setYAxis(false)
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        lineChartView.show()
        val barChartView: BarChartView =
            getBackgroundBarChart(R.id.graph_windspeed_background, 0, max, false)
        barChartView.show()
    }

    private fun pressureGraph() {
        val lineChartView: LineChartView = findViewById(R.id.graph_pressure)
        var minPressure = 100000f
        var maxPressure = 0f
        val dataset = LineSet()
        for (i in 0 until numWeatherData) {
            val pressure: Float = UnitConvertor.convertPressure(weatherList[i].pressure, sp)
            minPressure =
                min(floor(pressure.toDouble()), minPressure.toDouble()).toFloat()
            maxPressure =
                max(ceil(pressure.toDouble()), maxPressure.toDouble()).toFloat()
            dataset.addPoint(getDateLabel(weatherList[i], i), pressure)
        }
        dataset.setSmooth(false)
        dataset.setColor(Color.parseColor("#4CAF50"))
        dataset.setThickness(4F)
        val middle = Math.round(minPressure + (maxPressure - minPressure) / 2)
        val stepSize = max(
            1.0, ceil((abs((maxPressure - minPressure).toDouble()) / 4).toDouble())
                .toInt().toDouble()
        ).toInt()
        var min = middle - 2 * stepSize
        var max = middle + 2 * stepSize
        var rows = 4
        if (ceil(maxPressure.toDouble()) - floor(minPressure.toDouble()) <= 3) {
            min = floor(minPressure.toDouble()).toInt()
            max = max(
                (min + 1).toDouble(), ceil(maxPressure.toDouble()).toInt()
                    .toDouble()
            )
                .toInt()
            rows = max - min
        }
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(dataset)
        lineChartView.addData(data)
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, rows, 1, gridPaint)
        lineChartView.setAxisBorderValues(min, max)
        lineChartView.setStep(stepSize)
        lineChartView.setLabelsColor(Color.parseColor(labelColor))
        lineChartView.setXAxis(false)
        lineChartView.setYAxis(false)
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        lineChartView.show()
        val barChartView: BarChartView =
            getBackgroundBarChart(R.id.graph_pressure_background, min, max, false)
        barChartView.show()
    }

    private fun humidityGraph() {
        val lineChartView: LineChartView = findViewById(R.id.graph_humidity)
        var minHumidity = 100000f
        var maxHumidity = 0f
        val dataset = LineSet()
        for (i in 0 until numWeatherData) {
            val humidity: Int = weatherList[i].humidity
            minHumidity = min(humidity.toDouble(), minHumidity.toDouble()).toFloat()
            maxHumidity = max(humidity.toDouble(), maxHumidity.toDouble()).toFloat()
            dataset.addPoint(getDateLabel(weatherList[i], i), humidity.toFloat())
        }
        dataset.setSmooth(false)
        dataset.setColor(Color.parseColor("#2196F3"))
        dataset.setThickness(4F)
        var min = minHumidity.toInt() / 10 * 10
        var max = ceil((maxHumidity / 10).toDouble()).toInt() * 10
        if (min == max) {
            max = min((max + 10).toDouble(), 100.0).toInt()
            min = max((min - 10).toDouble(), 0.0).toInt()
        }
        val stepSize = if (max - min == 100) 20 else 10
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(dataset)
        lineChartView.addData(data)
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, (max - min) / stepSize, 1, gridPaint)
        lineChartView.setAxisBorderValues(min, max)
        lineChartView.setStep(stepSize)
        lineChartView.setLabelsColor(Color.parseColor(labelColor))
        lineChartView.setXAxis(false)
        lineChartView.setYAxis(false)
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        lineChartView.show()
        val barChartView: BarChartView =
            getBackgroundBarChart(R.id.graph_humidity_background, min, max, false)
        barChartView.show()
    }

    private fun parseLongTermJson(result: String?): ParseResult {
        try {
            val parsedWeatherList: List<Weather> =
                OpenWeatherMapJsonParser.convertJsonToWeatherList(result)
            weatherList.addAll(parsedWeatherList)
        } catch (e: JSONException) {
            Log.e("JSONException Data", result!!)
            e.printStackTrace()
            return ParseResult.JSON_EXCEPTION
        }
        return ParseResult.OK
    }

    /**
     * Returns a label for the dates, only one per day preferably at noon.
     * @param weather weather entity
     * @param i number of weather in long term forecast
     * @return label (either short form of day in week or empty string)
     */
    private fun getDateLabel(weather: Weather, i: Int): String {
        val output = dateFormat.format(weather.date!!)
        val cal = Calendar.getInstance()
        cal.time = weather.date!!
        val weatherHour = cal[Calendar.HOUR_OF_DAY]

        // label for first day if it starts after 13:00
        return if (i == 0 && weatherHour > 13) {
            output
        } else if (i == numWeatherData - 1 && weatherHour < 11) {
            output
        } else if (weatherHour >= 11 && weatherHour <= 13) {
            output
        } else {
            ""
        }
    }

    /**
     * Returns a background chart with alternating vertical bars for each day.
     * @param id BarChartView resource id
     * @param min foreground chart min label
     * @param max foreground chart max label
     * @param includeLast true for foreground bar charts, false for foreground line charts
     * @return background bar chart
     */
    private fun getBackgroundBarChart(
        @IdRes id: Int,
        min: Int,
        max: Int,
        includeLast: Boolean
    ): BarChartView {
        var max = max
        var visible = false
        var lastHour = 25

        // get label with biggest visual length
        if (getLengthAsString(min) > getLengthAsString(max)) {
            max = min
        }
        val barDataset = BarSet()
        for (i in 0 until numWeatherData) {
            if (i != numWeatherData - 1 || includeLast) {
                for (j in 0..2) {
                    val cal = Calendar.getInstance()
                    cal.time = weatherList[i].date
                    val hour = cal[Calendar.HOUR_OF_DAY]

                    // 23:00 to 0:00 new day
                    if (hour < lastHour) {
                        visible = !visible
                    }
                    barDataset.addBar("", (if (visible) max else 0).toFloat())
                    lastHour = hour
                }
            }
        }
        barDataset.setColor(Color.parseColor(backgroundBarColor))
        barDataset.alpha = 0.075f
        val data: ArrayList<ChartSet> = ArrayList<ChartSet>()
        data.add(barDataset)
        val barChartView: BarChartView = findViewById(id)
        barChartView.addData(data)
        barChartView.setBarSpacing(0F) // visually join bars into one bar per day
        barChartView.setAxisBorderValues(min(0, max), max(0, max))
        barChartView.setLabelsColor(Color.parseColor("#00ffffff")) // fully transparent (= invisible) labels
        barChartView.setXAxis(false)
        barChartView.setYAxis(false)
        barChartView.setBorderSpacing(Tools.fromDpToPx(10F))
        return barChartView
    }

    /**
     * Returns a comparable abstract length/width an integer number uses as a chart label (works best for fonts with monospaced digits).
     * @param i number
     * @return length
     */
    private fun getLengthAsString(i: Int): Int {
        val array = i.toString().toCharArray()
        var sum = 0
        for (c in array) {
            sum += if (c == '-') 1 else 2 // minus is smaller than digits
        }
        return sum
    }
}

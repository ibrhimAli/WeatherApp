package com.musalasoft.weatherapp.models

import com.musalasoft.weatherapp.utils.formatters.WeatherFormatterType

class WeatherPresentation {
    /**
     * Returns `true` if temperature should be rounded and `false` if shouldn't.
     * @return `true` if temperature should be rounded and `false` if shouldn't
     */
    val isRoundedTemperature: Boolean

    /**
     * Returns temperature units as temperature unit key.
     * @return temperature units
     */
    val temperatureUnits: String

    /**
     * Returns wind speed units as wind speed unit key.
     * @return wind speed units
     */
    val windSpeedUnits: String

    /**
     * Returns wind direction format.
     * @return wind direction format
     */
    val windDirectionFormat: String

    /**
     * Returns pressure units as pressure unit key.
     * @return pressure units
     */
    val pressureUnits: String
    private val showTemperatureInStatusBar: Boolean

    /** Weather information.  */
    private val weather: ImmutableWeather
    private val type: WeatherFormatterType

    constructor() {
        isRoundedTemperature = DEFAULT_DO_ROUND_TEMPERATURE
        temperatureUnits = DEFAULT_TEMPERATURE_UNITS
        windSpeedUnits = DEFAULT_WIND_SPEED_UNITS
        windDirectionFormat = DEFAULT_WIND_DIRECTION_FORMAT
        pressureUnits = DEFAULT_PRESSURE_UNITS
        showTemperatureInStatusBar = DEFAULT_SHOW_TEMPERATURE_IN_STATUS_BAR
        weather = ImmutableWeather.EMPTY
        type = WeatherFormatterType.NOTIFICATION_SIMPLE
    }

    constructor(
        roundedTemperature: Boolean, temperatureUnits: String,
        windSpeedUnits: String, windDirectionFormat: String,
        pressureUnits: String, showTemperatureInStatusBar: Boolean,
        weather: ImmutableWeather,
        type: WeatherFormatterType
    ) {
        isRoundedTemperature = roundedTemperature
        this.temperatureUnits = temperatureUnits
        this.windSpeedUnits = windSpeedUnits
        this.windDirectionFormat = windDirectionFormat
        this.pressureUnits = pressureUnits
        this.showTemperatureInStatusBar = showTemperatureInStatusBar
        this.weather = weather
        this.type = type
    }

    /**
     * Returns `true` if temperature should be shown in status bar and `false` if shouldn't.
     * @return `true` if temperature should be shown in status bar and `false` if shouldn't
     */
    fun shouldShowTemperatureInStatusBar(): Boolean {
        return showTemperatureInStatusBar
    }

    /**
     * Returns weather information.
     * @return weather information
     */
    fun getWeather(): ImmutableWeather {
        return weather
    }

    /**
     * Returns weather formatter type.
     * @return weather formatter type
     */
    fun getType(): WeatherFormatterType {
        return type
    }

    fun copy(roundedTemperature: Boolean): WeatherPresentation {
        return WeatherPresentation(
            roundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copyTemperatureUnits(temperatureUnits: String): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copyWindSpeedUnits(windSpeedUnits: String): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copyWindDirectionFormat(windDirectionFormat: String): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copyPressureUnits(pressureUnits: String): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copyShowTemperatureInStatusBar(showTemperatureInStatusBar: Boolean): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copy(weather: ImmutableWeather): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    fun copy(type: WeatherFormatterType): WeatherPresentation {
        return WeatherPresentation(
            isRoundedTemperature, temperatureUnits, windSpeedUnits,
            windDirectionFormat, pressureUnits, showTemperatureInStatusBar,
            weather, type
        )
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is WeatherPresentation) return false
        val that = o
        if (isRoundedTemperature != that.isRoundedTemperature) return false
        if (temperatureUnits != that.temperatureUnits) return false
        if (windSpeedUnits != that.windSpeedUnits) return false
        if (windDirectionFormat != that.windDirectionFormat) return false
        if (pressureUnits != that.pressureUnits) return false
        if (!weather.equals(that.weather)) return false
        return if (showTemperatureInStatusBar != that.showTemperatureInStatusBar) false else type === that.type
    }

    override fun hashCode(): Int {
        var result = if (isRoundedTemperature) 1 else 0
        result = 31 * result + temperatureUnits.hashCode()
        result = 31 * result + windSpeedUnits.hashCode()
        result = 31 * result + windDirectionFormat.hashCode()
        result = 31 * result + pressureUnits.hashCode()
        result = 31 * result + if (showTemperatureInStatusBar) 1 else 0
        result = 31 * result + weather.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "WeatherPresentation{" +
                "roundedTemperature=" + isRoundedTemperature +
                ", temperatureUnits='" + temperatureUnits + '\'' +
                ", windSpeedUnits='" + windSpeedUnits + '\'' +
                ", windDirectionFormat='" + windDirectionFormat + '\'' +
                ", pressureUnits='" + pressureUnits + '\'' +
                ", shouldShowTemperatureInStatusBar=" + showTemperatureInStatusBar +
                ", weather=" + weather +
                ", type=" + type +
                '}'
    }

    companion object {
        /** Do not round temperature by default.  */
        const val DEFAULT_DO_ROUND_TEMPERATURE = false

        /** Default temperature unit is Celsius.  */
        const val DEFAULT_TEMPERATURE_UNITS = "°C"

        /** Default wind speed unit is meters per second.  */
        const val DEFAULT_WIND_SPEED_UNITS = "m/s"

        /** Default wind direction format is arrows.  */
        const val DEFAULT_WIND_DIRECTION_FORMAT = "arrow"

        /** Default pressure units is hPa/mBar.  */
        const val DEFAULT_PRESSURE_UNITS = "hPa/mBar"

        /** Show temperature in status bar by default.  */
        const val DEFAULT_SHOW_TEMPERATURE_IN_STATUS_BAR = true
    }
}
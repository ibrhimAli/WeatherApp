package com.musalasoft.weatherapp.utils.localizers

import android.content.Context
import androidx.annotation.StringRes
import com.musalasoft.weatherapp.R

/**
 * Class to localize (translate) wind speed units to current locale.
 */
// TODO replace "singleton" with DI
object WindSpeedUnitsLocalizer {
    /**
     * Localize `units` to current locale.
     * @param units wind speed units
     * @return string resource for `units`
     * @throws NullPointerException if `units` is null
     * @throws IllegalArgumentException if `units` have value other than "m/s", "kph", "mph" or "kn"
     */
    // TODO replace String with enum
    @StringRes
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun localizeWindSpeedUnits(units: String): Int {
        if (units == null) throw NullPointerException("units should not be null")
        return when (units) {
            "m/s" -> R.string.speed_unit_mps
            "kph" -> R.string.speed_unit_kph
            "mph" -> R.string.speed_unit_mph
            "kn" -> R.string.speed_unit_kn
            else -> throw IllegalArgumentException("Unknown units: \"$units\"")
        }
    }

    /**
     * Localize `units` to current locale.
     * @param units wind speed units
     * @param context android context
     * @return string for `units`
     * @throws NullPointerException if any of parameters is null
     * @throws IllegalArgumentException if `units` have value other than "m/s", "kph", "mph" or "kn"
     */
    // TODO replace String with enum
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun localizeWindSpeedUnits(units: String, context: Context): String {
        if (units == null) throw NullPointerException("units should not be null")
        if (context == null) throw NullPointerException("context should not be null")
        val unitsResourceId = localizeWindSpeedUnits(units)
        return context.getString(unitsResourceId)
    }
}

package com.musalasoft.weatherapp.utils.localizers

import android.content.Context
import androidx.annotation.StringRes
import com.musalasoft.weatherapp.R

/**
 * Class to localize (translate) pressure units to current locale.
 */
// TODO replace "singleton" with DI
object PressureUnitsLocalizer {
    /**
     * Localize `units` to current locale.
     * @param units pressure units
     * @return string resource for `units`
     * @throws NullPointerException if `units` is null
     * @throws IllegalArgumentException if `units` have value other than "hPa", "hPa/mBar",
     * "kPa", "mm Hg" or "in Hg"
     */
    // TODO replace String with enum
    @StringRes
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun localizePressureUnits(units: String): Int {
        if (units == null) throw NullPointerException("units should not be null")
        return when (units) {
            "hPa", "hPa/mBar" -> R.string.pressure_unit_hpa
            "kPa" -> R.string.pressure_unit_kpa
            "mm Hg" -> R.string.pressure_unit_mmhg
            "in Hg" -> R.string.pressure_unit_inhg
            else -> throw IllegalArgumentException("Unknown units: \"$units\"")
        }
    }

    /**
     * Localize `units` to current locale.
     * @param units pressure units
     * @param context android context
     * @return string for `units`
     * @throws NullPointerException if any of parameters is null
     * @throws IllegalArgumentException if `units` have value other than "hPa", "hPa/mBar",
     * "kPa", "mm Hg" or "in Hg"
     */
    // TODO replace String with enum
    @Throws(NullPointerException::class, IllegalArgumentException::class)
    fun localizePressureUnits(units: String, context: Context): String {
        if (units == null) throw NullPointerException("units should not be null")
        if (context == null) throw NullPointerException("context should not be null")
        val unitsResourceId = localizePressureUnits(units)
        return context.getString(unitsResourceId)
    }
}

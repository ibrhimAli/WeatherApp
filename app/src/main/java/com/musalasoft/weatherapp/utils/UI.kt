package com.musalasoft.weatherapp.utils

import com.musalasoft.weatherapp.R
import android.app.Activity
import android.os.Build
import android.view.View

object UI {
    fun setNavigationBarMode(context: Activity, darkTheme: Boolean, blackTheme: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val content = context.findViewById<View>(android.R.id.content)
            if (!darkTheme && !blackTheme) {
                content.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }

    fun getTheme(themePref: String?): Int {
        return when (themePref) {
            "dark" -> R.style.AppTheme_NoActionBar_Dark
            "black" -> R.style.AppTheme_NoActionBar_Black
            "classic" -> R.style.AppTheme_NoActionBar_Classic
            "classicdark" -> R.style.AppTheme_NoActionBar_Classic_Dark
            "classicblack" -> R.style.AppTheme_NoActionBar_Classic_Black
            else -> R.style.AppTheme_NoActionBar
        }
    }
}

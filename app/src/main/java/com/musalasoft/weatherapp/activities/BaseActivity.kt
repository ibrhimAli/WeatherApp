package com.musalasoft.weatherapp.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.musalasoft.weatherapp.R
import com.musalasoft.weatherapp.utils.UI

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    protected open var theme: Int? = null
    protected open var darkTheme = false
    protected var blackTheme = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(UI.getTheme(prefs.getString("theme", "fresh")).also { theme = it })
        darkTheme = theme == R.style.AppTheme_NoActionBar_Dark ||
                theme == R.style.AppTheme_NoActionBar_Classic_Dark
        blackTheme = theme == R.style.AppTheme_NoActionBar_Black ||
                theme == R.style.AppTheme_NoActionBar_Classic_Black
        UI.setNavigationBarMode(this@BaseActivity, darkTheme, blackTheme)
    }
}

package com.musalasoft.weatherapp.utils

import java.util.Locale

object Language {
    val owmLanguage: String
        /**
         * Returns the language code used by OpenWeatherMap corresponding to the system's default
         * language.
         * @return language code
         */
        get() {
            val language = Locale.getDefault().language
            return if (language == Locale("cs").language) { // Czech
                "cz"
            } else if (language == Locale("ko").language) { // Korean
                "kr"
            } else if (language == Locale("lv").language) { // Latvian
                "la"
            } else {
                language
            }
        }
}

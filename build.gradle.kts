// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id(libs.plugins.androidApplication.get().pluginId) apply false
    id(libs.plugins.jetbrainsKotlinAndroid.get().pluginId) apply false
    id(libs.plugins.androidNavigation.get().pluginId) apply false
    id(libs.plugins.kotlinKapt.get().pluginId) apply false
    id(libs.plugins.kotlinParcelize.get().pluginId) apply false
}
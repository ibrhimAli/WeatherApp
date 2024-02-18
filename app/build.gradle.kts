plugins {
    id(libs.plugins.androidApplication.get().pluginId)
    id(libs.plugins.jetbrainsKotlinAndroid.get().pluginId)
    id(libs.plugins.androidNavigation.get().pluginId)
    id(libs.plugins.kotlinKapt.get().pluginId)
    id(libs.plugins.kotlinParcelize.get().pluginId)
}



android {
    namespace = "com.musalasoft.weatherapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.musalasoft.weatherapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        dataBinding = true
    }

//    packaging {
//        resources {
//            excludes.add("**/attach_hotspot_windows.dll")
//            excludes.add("META-INF/licenses/**")
//            excludes.add("META-INF/AL2.0")
//            excludes.add("META-INF/LGPL2.1")
//        }
//    }
}

dependencies {
    // kotlin
    implementation(libs.kotlin)
    implementation(libs.kotlinReflect)

    // android supports
    implementation(libs.supportv4)
    implementation(libs.appCompat)
    implementation(libs.materialDesign)
    implementation(libs.recyclerView)
    implementation(libs.cardView)
    implementation(libs.coreKtx)
    implementation(libs.diogobernardino.williamchart)

    // architecture components
    implementation(libs.navigationUI)
    implementation(libs.navigationFragment)
    implementation(libs.lifecycleExt)
    implementation(libs.liveDataKtx)
    implementation(libs.lifecycleViewModelKtx)
    implementation(libs.roomRuntime)
    kapt(libs.roomCompiler)
    kapt(libs.lifecycleAnnotation)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
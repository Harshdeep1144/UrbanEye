package com.example.urbaneye

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class UrbanEyeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName
        val osmConfig = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = osmConfig
    }
}

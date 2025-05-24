package net.marfanet.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MarFaNetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any required components here
    }
}

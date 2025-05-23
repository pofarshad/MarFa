package net.marfanet.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MarFaNetApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Analytics
        FirebaseAnalytics.getInstance(this)
        
        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance()
    }
}
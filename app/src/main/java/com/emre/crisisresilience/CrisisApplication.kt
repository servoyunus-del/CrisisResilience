package com.emre.crisisresilience

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CrisisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

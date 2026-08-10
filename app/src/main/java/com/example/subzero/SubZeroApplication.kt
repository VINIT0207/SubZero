package com.example.subzero

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

class SubZeroApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Manually install MultiDex to ensure the secondary DEX files 
        // are indexed before the Launcher Activity is instantiated.
        MultiDex.install(this)
    }
}

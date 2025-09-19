package com.danieljm.delijn

import android.app.Application
import com.danieljm.delijn.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class DeLijnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@DeLijnApp)
            modules(appModules)
        }
    }
}

package com.adityapk.zcash.zqwandroid

import android.app.Application
import android.content.Context

class ZQWApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ZQWApp.appContext = applicationContext
    }

    companion object {

        var appContext: Context? = null
            private set
    }
}

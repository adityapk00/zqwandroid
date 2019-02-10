package com.adityapk.zcash.zqwandroid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        updateUI()

        btnDisconnect.setOnClickListener {
            DataModel.setConnString(null, applicationContext)
            DataModel.clear()
            updateUI()
        }

        chkDisallowInternet.setOnClickListener {
            DataModel.setGlobalAllowInternet(chkDisallowInternet.isChecked)
        }
    }

    fun updateUI() {
        txtSettingsConnString.text = DataModel.getConnString(ZQWApp.appContext!!) ?: "Not Connected"

        lblVersionName.text = BuildConfig.VERSION_NAME
        lblServerVersion.text = DataModel.mainResponseData?.serverversion ?: "Not Connected"
    }
}

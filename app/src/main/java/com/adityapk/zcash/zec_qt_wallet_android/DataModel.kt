package com.adityapk.zcash.zec_qt_wallet_android

import com.beust.klaxon.Klaxon

object DataModel {
    class MainReponse(val balance: Double, val saplingAddress: String, val zecprice: Double)

    var mainResponseData : MainReponse? = null

    fun parseResponse(response: String) {
        mainResponseData = Klaxon().parse<MainReponse>(response)
    }

}
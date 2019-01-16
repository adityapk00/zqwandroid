package com.adityapk.zcash.zec_qt_wallet_android

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser

object DataModel {
    class MainReponse(val balance: Double, val saplingAddress: String, val zecprice: Double)

    var mainResponseData : MainReponse? = null
    var transactions : List<JsonObject> ?= null

    fun parseResponse(response: String) {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject
        when (json.string("command")) {
            "getInfo" -> mainResponseData = Klaxon().parse<MainReponse>(response)
            "getTransactions" -> {
                transactions = json.array("transactions")
            }
        }
    }

}
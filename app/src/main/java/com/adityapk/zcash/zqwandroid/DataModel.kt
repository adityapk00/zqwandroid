package com.adityapk.zcash.zqwandroid

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser

object DataModel {
    class MainResponse(val balance: Double, val saplingAddress: String, val zecprice: Double)


    var mainResponseData : MainResponse? = null
    var transactions : List<JsonObject> ?= null

    fun parseResponse(response: String) {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject
        when (json.string("command")) {
            "getInfo" -> mainResponseData = Klaxon().parse<MainResponse>(response)
            "getTransactions" -> {
                transactions = json.array("transactions")
            }
        }
    }

}
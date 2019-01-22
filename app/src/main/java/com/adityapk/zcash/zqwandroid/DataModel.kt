package com.adityapk.zcash.zqwandroid

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import android.R.id.edit
import android.content.Context
import android.content.SharedPreferences



object DataModel {
    class MainResponse(val balance: Double, val saplingAddress: String, val zecprice: Double)
    class TransactionItem(val type: String, val datetime: Long, val amount: String, val memo: String?,
                          val addr: String, val txid: String?, val confirmations: Long)

    var mainResponseData : MainResponse? = null
    var transactions : List<TransactionItem> ?= null

    fun clear() {
        mainResponseData = null
        transactions = null
    }

    fun parseResponse(response: String) {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject
        when (json.string("command")) {
            "getInfo" -> mainResponseData = Klaxon().parse<MainResponse>(response)
            "getTransactions" -> {
                transactions = json.array<JsonObject>("transactions").orEmpty().map { tx ->
                    TransactionItem(
                        tx.string("type") ?: "",
                        tx.long("datetime") ?: 0,
                        tx.string("amount") ?: "0",
                        tx.string("memo") ?: "",
                        tx.string("address") ?: "",
                        tx.string("txid") ?: "",
                        tx.long("confirmations") ?: 0)
                }
            }
        }
    }

    fun setConnString(value: String?, context: Context) {
        val settings = context.getSharedPreferences("ConnInfo", 0)
        val editor = settings.edit()
        editor.putString("connstring", value)
        editor.apply()
    }

    fun getConnString(context: Context) : String? {
        val settings = context.getSharedPreferences("ConnInfo", 0)
        return settings.getString("connstring", null)
    }
}
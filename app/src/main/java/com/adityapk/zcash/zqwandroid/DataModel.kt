package com.adityapk.zcash.zqwandroid

import android.content.Context
import android.util.Log
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import okhttp3.WebSocket


object DataModel {
    class MainResponse(val balance: Double, val saplingAddress: String, val zecprice: Double)
    class TransactionItem(val type: String, val datetime: Long, val amount: String, val memo: String?,
                          val addr: String, val txid: String?, val confirmations: Long)

    var mainResponseData : MainResponse? = null
    var transactions : List<TransactionItem> ?= null

    var ws : WebSocket? = null

    fun clear() {
        mainResponseData = null
        transactions = null
    }

    fun parseResponse(response: String) : Boolean {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject

        return when (json.string("command")) {
            "getInfo" -> {
                mainResponseData = Klaxon().parse<MainResponse>(response)
                return false
            }
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
                return true
            }
            else -> false
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

    fun sendTx(tx: TransactionItem) {
        val payload = json { obj("command" to "sendTx", "tx" to obj(
            "amount" to tx.amount,
            "to" to tx.addr,
            "memo" to tx.memo
        )) }

        Log.w(TAG, payload.toJsonString(true))
        ws?.send(payload.toJsonString())
    }

    fun makeAPICalls() {
        ws?.send(json { obj("command" to "getInfo") }.toJsonString())
        ws?.send(json { obj("command" to "getTransactions")}.toJsonString())
    }


    fun isValidAddress(a: String) : Boolean {
        return  Regex("^z[a-z0-9]{77}$", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^ztestsapling[a-z0-9]{76}", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^t[a-z0-9]{34}$", RegexOption.IGNORE_CASE).matches(a)

    }

    private val TAG = "DataModel"

}
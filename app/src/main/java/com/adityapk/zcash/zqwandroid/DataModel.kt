package com.adityapk.zcash.zqwandroid

import android.content.Context
import android.util.Log
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.beust.klaxon.json
import okhttp3.WebSocket
import org.libsodium.jni.NaCl
import org.libsodium.jni.Sodium
import java.math.BigInteger

object DataModel {
    class MainResponse(val balance: Double, val maxspendable: Double, val maxzspendable: Double? = null,
                       val saplingAddress: String, val tAddress: String, val zecprice: Double, val tokenName: String,
                       val serverversion: String)

    class TransactionItem(val type: String, val datetime: Long, val amount: String, val memo: String?,
                          val addr: String, val txid: String?, val confirmations: Long)

    var mainResponseData : MainResponse? = null
    var transactions : List<TransactionItem> ?= null

    fun isTestnet(): Boolean {
        return mainResponseData?.tokenName != "ZEC"
    }

    var ws : WebSocket? = null


    enum class ConnectionStatus(val status: Int) {
        DISCONNECTED(1),
        CONNECTING(2),
        CONNECTED(3)
    }

    var connStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED

    fun clear() {
        mainResponseData = null
        transactions = null
    }

    fun ByteArray.toHexString() : String {
        return (joinToString("") { String.format("%02x", it) })
    }

    fun String.hexStringToByteArray(byteSize: Int) : ByteArray {
        val s = "00".repeat(byteSize - (this.length / 2) ) + this
        return ByteArray(byteSize) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    fun init() {
        val sodium = NaCl.sodium()
    }

    data class ParseResponse(val updateTxns: Boolean = false, val displayMsg: String? = null, val doDisconnect: Boolean = false)

    fun parseResponse(response: String) : ParseResponse {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject

        // Check if input string is encrypted
        if (json.containsKey("nonce")) {
            val decrypted = decrypt(json["nonce"].toString(), json["payload"].toString())
            if (decrypted.startsWith("error")) {
                return ParseResponse(false, "Encryption Error: $decrypted", true)
            }
            return parseResponse(decrypted)
        }

        // Check if it has errored out
        if (json.containsKey("error")) {
            return ParseResponse(false, "Couldn't connect: ${json["error"].toString()}", true)
        }

        return when (json.string("command")) {
            "getInfo" -> {
                mainResponseData = Klaxon().parse<MainResponse>(response)

                // Call the next API call
                ws?.send(encrypt(json { obj("command" to "getTransactions") }.toJsonString()))
                return ParseResponse()
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
                return ParseResponse(true)
            }
            "sendTx" -> {
                // Ignore
                return ParseResponse()
            }
            "sendTxSubmitted" -> {
                val txid = json.string("txid")
                return ParseResponse(false, "Tx submitted: $txid")
            }
            "sendTxFailed" -> {
                val err = json.string("err")
                return ParseResponse(false, "Tx displayMsg: $err")
            }
            else -> {
                Log.e(TAG, "Unknown command ${json.string("command")}")
                return ParseResponse()
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

    fun sendTx(tx: TransactionItem) {
        val payload = json { obj("command" to "sendTx", "tx" to obj(
            "amount" to tx.amount,
            "to" to tx.addr,
            "memo" to tx.memo
        )) }

        Log.w(TAG, payload.toJsonString(true))
        ws?.send(encrypt(payload.toJsonString()))
    }

    fun makeAPICalls() {
        if (getSecret() == null) {
            // Connected, but we don't have a secret, so we can't actually connect.
            ws?.close(1000, "No shared secret, can't connect")
        } else {
            // We make only the first API call here. The subsequent ones are made in parseResponsegit (), when this
            // call returns a reply
            val phoneName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
            ws?.send(encrypt(json { obj("command" to "getInfo", "name" to phoneName) }.toJsonString()))
        }
    }

    fun isValidAddress(a: String) : Boolean {
        return if (isTestnet()) {
            Regex("^ztestsapling[a-z0-9]{76}", RegexOption.IGNORE_CASE).matches(a) ||
                    Regex("^tm[a-z0-9]{33}$", RegexOption.IGNORE_CASE).matches(a)
        } else {
            Regex("^z[a-z0-9]{77}$", RegexOption.IGNORE_CASE).matches(a) ||
                    Regex("^t[a-z0-9]{34}$", RegexOption.IGNORE_CASE).matches(a)
        }

    }

    private fun decrypt(nonceHex: String, encHex: String) : String {
        // Enforce limits on sizes
        if (nonceHex.length > Sodium.crypto_secretbox_noncebytes() *2 ||
            encHex.length > 2 * 50 * 1024 /*50kb*/) {
            return "error: Max size of message exceeded"
        }

        // First make sure the remote nonce is valid
        if (!checkRemoteNonce(nonceHex)) {
            return "error: Remote Nonce was too low"
        }

        val encsize = encHex.length / 2
        val encbin = encHex.hexStringToByteArray(encHex.length / 2)

        val decrypted = ByteArray(encsize - Sodium.crypto_secretbox_macbytes())

        val noncebin = nonceHex.hexStringToByteArray(Sodium.crypto_secretbox_noncebytes())

        val result = Sodium.crypto_secretbox_open_easy(decrypted, encbin, encsize, noncebin, getSecret())
        if (result != 0) {
            return "error: Decryption Error"
        }

        Log.i(this.TAG, "Decrypted to: ${String(decrypted).replace("\n", " ")}")
        updateRemoteNonce(nonceHex)
        return String(decrypted)
    }

    private fun encrypt(s : String) : String {
        // Pad to 256 bytes, to prevent leaking any info via size of the encrypted message
        var inpStr = s
        if (inpStr.length % 256 > 0) {
            inpStr += " ".repeat(256 - (inpStr.length % 256))
        }

        // Take the string, encrypt it and send it as the payload with the nonce in a Json string
        val msg = inpStr.toByteArray()

        check(getSecret() != null)

        val encrypted = ByteArray(msg.size + Sodium.crypto_secretbox_macbytes())

        // Increment nonce
        val localNonce = incAndGetLocalNonce()

        val ret = Sodium.crypto_secretbox_easy(encrypted, msg, msg.size, localNonce, getSecret())
        if (ret != 0) {
            println("Encryption failed")
        }

        val j = json { obj("nonce" to localNonce.toHexString(),
                          ("payload" to encrypted.toHexString()),
                          ("to" to getWormholeCode())
                        )}
        println("Sending ${j.toJsonString()}")

        return j.toJsonString()
    }

    private fun checkRemoteNonce(remoteNonce: String): Boolean {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        val prevNonceHex = settings.getString("remotenonce", "00".repeat(Sodium.crypto_secretbox_noncebytes()))!!

        // The problem is the nonces are hex encoded in little endian, but the BigDecimal contructor expects the nonces
        // in big endian format. So flip the endian-ness of the hex strings for comparision

        return BigInteger(remoteNonce.chunked(2).reversed().joinToString(""),16) >
                BigInteger(prevNonceHex.chunked(2).reversed().joinToString(""), 16)
    }

    private fun updateRemoteNonce(remoteNonce: String) {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        val editor = settings.edit()
        editor.putString("remotenonce", remoteNonce)
        editor.apply()
    }

    private fun incAndGetLocalNonce() : ByteArray {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        val nonceHex = settings.getString("localnonce", "00".repeat(Sodium.crypto_secretbox_noncebytes()))

        val nonce = nonceHex!!.hexStringToByteArray(Sodium.crypto_secretbox_noncebytes())
        Sodium.sodium_increment(nonce, nonce.size)
        Sodium.sodium_increment(nonce, nonce.size)

        val editor = settings.edit()
        editor.putString("localnonce", nonce.toHexString())
        editor.apply()

        return nonce
    }

    fun getWormholeCode() : String? {
        if (getSecret() == null)
            return null

        val tobin1 = ByteArray(Sodium.crypto_hash_sha256_bytes())
        Sodium.crypto_hash_sha256(tobin1, getSecret(), getSecret()!!.size)

        val tobin2 = ByteArray(Sodium.crypto_hash_sha256_bytes())
        Sodium.crypto_hash_sha256(tobin2, tobin1, tobin1.size)

        return tobin2.toHexString()

    }

    fun setSecretHex(secretHex: String) {
        if (getSecret()?.toHexString() == secretHex) {
            return
        }

        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)

        val editor = settings.edit()
        editor.putString("secret", secretHex)
        editor.remove("localnonce")
        editor.remove("remotenonce")
        editor.apply()
    }

    fun getSecret() : ByteArray? {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        val secretHex = settings.getString("secret", "")

        if (secretHex.isNullOrEmpty()) {
            return null
        }

        return secretHex.hexStringToByteArray(Sodium.crypto_secretbox_keybytes())
    }


    fun setGlobalAllowInternet(allow: Boolean) {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)

        val editor = settings.edit()
        editor.putBoolean("globalallowinternet", allow)
        editor.apply()
    }

    fun getGlobalAllowInternet(): Boolean {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        return settings.getBoolean("globalallowinternet", true)
    }

    fun setAllowInternet(allow: Boolean) {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)

        val editor = settings.edit()
        editor.putBoolean("allowinternet", allow)
        editor.apply()
    }

    fun getAllowInternet(): Boolean {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Secret", 0)
        return settings.getBoolean("allowinternet", false)
    }

    private const val TAG = "DataModel"
}
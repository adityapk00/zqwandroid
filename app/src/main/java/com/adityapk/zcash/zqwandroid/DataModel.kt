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
    class MainResponse(val balance: Double, val saplingAddress: String, val tAddress: String, val zecprice: Double, val tokenName: String)
    class TransactionItem(val type: String, val datetime: Long, val amount: String, val memo: String?,
                          val addr: String, val txid: String?, val confirmations: Long)

    var mainResponseData : MainResponse? = null
    var transactions : List<TransactionItem> ?= null

    var ws : WebSocket? = null

    fun clear() {
        mainResponseData = null
        transactions = null
    }

    var secret : ByteArray? = null

    fun ByteArray.toHexString() : String {
        return (joinToString("") { String.format("%02x", it) })
    }

    fun String.hexStringToByteArray(byteSize: Int) : ByteArray {
        val s = "00".repeat(byteSize - (this.length / 2) ) + this
        return ByteArray(byteSize) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    fun init() {
        val sodium = NaCl.sodium()

        secret = ByteArray(Sodium.crypto_hash_sha256_bytes())
        Sodium.crypto_hash_sha256(secret, "secret".toByteArray(), "secret".toByteArray().size)

        /*
        val message = "test".toByteArray()
        val noncelen = Sodium.crypto_secretbox_noncebytes().toLong()
        val nonce = ByteArray(noncelen.toInt())
        val ciphertextlen = Sodium.crypto_secretbox_macbytes() + message.size
        val ciphertext = ByteArray(ciphertextlen)
        val secret = ByteArray(Sodium.crypto_secretbox_keybytes())

        Sodium.randombytes_buf(nonce, noncelen.toInt())
        Sodium.randombytes(secret, Sodium.crypto_secretbox_keybytes())

        println("Noncelen = $noncelen" )
        println(BigInteger(nonce.toHexString(), 16))

        var ret = Sodium.crypto_secretbox_easy(ciphertext, message, message.size, nonce, secret)
        Log.i(TAG, ret.toString())

        val decrypted = ByteArray(ciphertext.size - Sodium.crypto_secretbox_macbytes())
        ret = Sodium.crypto_secretbox_open_easy(decrypted, ciphertext, ciphertextlen, nonce, secret)

        Log.i(TAG, ret.toString())
        println("Recovered message=" + String(decrypted))
        */
    }

    fun parseResponse(response: String) : Boolean {
        val json = Parser.default().parse(StringBuilder(response)) as JsonObject

        // Check if input string is encrypted
        if (json.containsKey("nonce")) {
            return parseResponse(decrypt(json["nonce"].toString(), json["payload"].toString()))
        }

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
        ws?.send(encrypt(payload.toJsonString()))
    }

    fun makeAPICalls() {
        ws?.send(encrypt(json { obj("command" to "getInfo") }.toJsonString()))
        ws?.send(encrypt(json { obj("command" to "getTransactions")}.toJsonString()))
    }


    fun isValidAddress(a: String) : Boolean {
        return  Regex("^z[a-z0-9]{77}$", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^ztestsapling[a-z0-9]{76}", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^t[a-z0-9]{34}$", RegexOption.IGNORE_CASE).matches(a)

    }

    fun decrypt(nonceHex: String, encHex: String) : String {
        // Decrypt the hex string into a regular string and return

        // First make sure the remote nonce is valid
        checkAndUpdateRemoteNonce(nonceHex);

        val encsize = encHex.length / 2
        val encbin = encHex.hexStringToByteArray(encHex.length / 2)

        val decrypted = ByteArray(encsize - Sodium.crypto_secretbox_macbytes())

        val noncebin = nonceHex.hexStringToByteArray(Sodium.crypto_secretbox_noncebytes())


        Sodium.crypto_secretbox_open_easy(decrypted, encbin, encsize, noncebin, secret);

        val s = String(decrypted)

        println("Decrypted to: ${String(decrypted)}")
        return String(decrypted)
    }

    fun encrypt(s : String) : String {
        // Take the string, encrypt it and send it as the payload with the nonce in a Json string
        val msg = s.toByteArray()

        check(secret != null)

        val encrypted = ByteArray(msg.size + Sodium.crypto_secretbox_macbytes())

        // Increment nonce
        val localNonce = incAndGetLocalNonce()

        val ret = Sodium.crypto_secretbox_easy(encrypted, msg, msg.size, localNonce, secret)
        if (ret != 0) {
            println("Encryption failed")
        }

        val j = json { obj("nonce" to localNonce.toHexString(),
                          ("payload" to encrypted.toHexString()))}

        return j.toJsonString()
    }

    fun checkAndUpdateRemoteNonce(remoteNonce: String) {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Nonce", 0)
        val prevNonceHex = settings.getString("remotenonce", "00".repeat(Sodium.crypto_secretbox_noncebytes()))

        // The problem is the nonces are hex encoded in little endian, but the BigDecimal contructor expects the nonces
        // in big endian format. So flip the endian-ness of the hex strings for comparision

        check(BigInteger(remoteNonce.chunked(2).reversed().joinToString(""), 16) > BigInteger(prevNonceHex.chunked(2).reversed().joinToString(""), 16))

        val editor = settings.edit()
        editor.putString("remotenonce", remoteNonce)
        editor.apply()
    }

    fun incAndGetLocalNonce() : ByteArray {
        val settings = ZQWApp.appContext!!.getSharedPreferences("Nonce", 0)
        val nonceHex = settings.getString("localnonce", "00".repeat(Sodium.crypto_secretbox_noncebytes()))

        val nonce = nonceHex!!.hexStringToByteArray(Sodium.crypto_secretbox_noncebytes())
        Sodium.sodium_increment(nonce, nonce.size)
        Sodium.sodium_increment(nonce, nonce.size)

        val editor = settings.edit()
        editor.putString("localnonce", nonce.toHexString())
        editor.apply()

        return nonce
    }


    private val TAG = "DataModel"

}
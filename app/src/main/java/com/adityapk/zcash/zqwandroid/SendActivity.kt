package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.content_send.*
import java.text.DecimalFormat


class SendActivity : AppCompatActivity() {

    private val REQUEST_ADDRESS = 1
    private val REQUEST_CONFIRM = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Send Transaction"

        // Clear the valid address prompt
        txtValidAddress.text = ""

        imageButton.setOnClickListener { view ->
            val intent = Intent(this, QrReaderActivity::class.java)
            startActivityForResult(intent, REQUEST_ADDRESS)
        }

        sendAddress.addTextChangedListener (object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isValidAddress(s.toString())) {
                    txtValidAddress.text = "\u2713 Valid address"
                    txtValidAddress.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                } else {
                    txtValidAddress.text = "Not a valid address"
                    txtValidAddress.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
                }
            }
        })

        amountUSD.addTextChangedListener (object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun afterTextChanged(s: Editable?) {}

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val usd = s.toString().toDoubleOrNull()
                val zprice = DataModel.mainResponseData?.zecprice
                if (usd == null || zprice == null)
                    amountZEC.text = "ZEC 0.0"
                else
                    amountZEC.text = "ZEC " + DecimalFormat("#.########").format(usd / zprice)
            }
        })

        btnSend.setOnClickListener { view ->
            val intent = Intent(this, TxDetailsActivity::class.java)
            val amt = amountZEC.text.toString()
            val parsedAmt = amt.substring("ZEC ".length, amt.length)
            val tx = DataModel.TransactionItem("confirm", 0, parsedAmt,
                txtSendMemo.text.toString(), sendAddress.text.toString(), "", 0)
            intent.putExtra("EXTRA_TXDETAILS", Klaxon().toJsonString(tx))
            startActivityForResult(intent, REQUEST_CONFIRM)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ADDRESS -> {
                if (resultCode == Activity.RESULT_OK) {
                    sendAddress.setText(data?.dataString ?: "nothing", TextView.BufferType.EDITABLE)

                    amountUSD.requestFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
                }
            }
            REQUEST_CONFIRM -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Send
                    finish()
                } else {
                    // Cancel
                }
            }
        }
    }

    fun isValidAddress(a: String) : Boolean {
        return  Regex("^z[a-z0-9]{77}$", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^ztestsapling[a-z0-9]{76}", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^t[a-z0-9]{34}$", RegexOption.IGNORE_CASE).matches(a)

    }

}

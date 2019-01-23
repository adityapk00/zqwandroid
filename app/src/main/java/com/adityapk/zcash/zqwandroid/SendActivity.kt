package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.activity_send.*
import kotlinx.android.synthetic.main.content_send.*
import java.text.DecimalFormat


class SendActivity : AppCompatActivity() {

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
            startActivityForResult(intent, QrReaderActivity.REQUEST_ADDRESS);
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

                if (s?.startsWith("t") == true) {
                    txtSendMemo.inputType = InputType.TYPE_NULL
                    txtSendMemo.text = SpannableStringBuilder("")
                    txtSendMemoTitle.text = "(No Memo for t-Addresses)"
                } else {
                    txtSendMemo.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    txtSendMemoTitle.text = "Memo (Optional)"
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

        txtSendMemo.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                txtMemoSize.text = "${s?.length ?: 0} / 512"
                if (s?.length ?: 0 > 512) {
                    txtMemoSize.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
                } else {
                    txtMemoSize.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                }
            }
        })

        btnSend.setOnClickListener { view ->
            // First, check if the address is correct.
            val toAddr = sendAddress.text.toString()
            if (!isValidAddress(toAddr)) {
                showErrorDialog("Invalid destination address!")
                return@setOnClickListener
            }

            val amt = amountZEC.text.toString()
            val parsedAmt = amt.substring("ZEC ".length, amt.length)
            if (parsedAmt.toDoubleOrNull() == 0.0 || parsedAmt.toDoubleOrNull() == null) {
                showErrorDialog("Invalid amount!")
                return@setOnClickListener
            }

            val memo = txtSendMemo.text.toString()
            if (memo.length > 512) {
                showErrorDialog("Memo is too long")
                return@setOnClickListener
            }

            if (toAddr.startsWith("t") && !memo.isBlank()) {
                showErrorDialog("Can't send a memo to a t-Address")
                return@setOnClickListener
            }

            val intent = Intent(this, TxDetailsActivity::class.java)
            val tx = DataModel.TransactionItem("confirm", 0, parsedAmt, memo,
                toAddr, "", 0)
            intent.putExtra("EXTRA_TXDETAILS", Klaxon().toJsonString(tx))
            startActivityForResult(intent, REQUEST_CONFIRM)
        }
    }

    fun showErrorDialog(msg: String) {
        val alertDialog = AlertDialog.Builder(this@SendActivity).create()
        alertDialog.setTitle("Error Sending Transaction")
        alertDialog.setMessage(msg)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") {
                dialog, _ -> dialog.dismiss() }
        alertDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            QrReaderActivity.REQUEST_ADDRESS -> {
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
                    val tx = Klaxon().parse<DataModel.TransactionItem>(data?.dataString!!)
                    DataModel.sendTx(tx!!)

                    finish()
                } else {
                    // Cancel
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

    fun isValidAddress(a: String) : Boolean {
        return  Regex("^z[a-z0-9]{77}$", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^ztestsapling[a-z0-9]{76}", RegexOption.IGNORE_CASE).matches(a) ||
                Regex("^t[a-z0-9]{34}$", RegexOption.IGNORE_CASE).matches(a)

    }

}

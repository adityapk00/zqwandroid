package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.*
import android.view.inputmethod.EditorInfo
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
        txtSendCurrencySymbol.text = ""

        imageButton.setOnClickListener { view ->
            val intent = Intent(this, QrReaderActivity::class.java)
            intent.putExtra("REQUEST_CODE", QrReaderActivity.REQUEST_ADDRESS)
            startActivityForResult(intent, QrReaderActivity.REQUEST_ADDRESS)
        }

        sendAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (DataModel.isValidAddress(s.toString())) {
                    txtValidAddress.text = "\u2713 Valid address"
                    txtValidAddress.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                } else {
                    txtValidAddress.text = "Not a valid address"
                    txtValidAddress.setTextColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
                }

                if (s?.startsWith("t") == true) {
                    txtSendMemo.isEnabled = false
                    txtSendMemo.text = SpannableStringBuilder("")
                    txtSendMemoTitle.text = "(No Memo for t-Addresses)"
                } else {
                    txtSendMemo.isEnabled = true
                    txtSendMemoTitle.text = "Memo (Optional)"
                }
            }
        })

        amountUSD.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val usd = s.toString().toDoubleOrNull()
                val zprice = DataModel.mainResponseData?.zecprice

                if (usd == null) {
                    txtSendCurrencySymbol.text = "" // Let the placeholder show the "$" sign
                } else {
                    txtSendCurrencySymbol.text = "$"
                }

                if (usd == null || zprice == null)
                    amountZEC.text = "${DataModel.mainResponseData?.tokenName} 0.0"
                else
                    amountZEC.text =
                        "${DataModel.mainResponseData?.tokenName} " + DecimalFormat("#.########").format(usd / zprice)
            }
        })

        txtSendMemo.setImeOptions(EditorInfo.IME_ACTION_DONE);
        txtSendMemo.setRawInputType(InputType.TYPE_CLASS_TEXT);

        txtSendMemo.addTextChangedListener(object : TextWatcher {
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
            doValidationsThenConfirm()
        }
    }

    private fun doValidationsThenConfirm()  {
        // First, check if the address is correct.
        val toAddr = sendAddress.text.toString()
        if (!DataModel.isValidAddress(toAddr)) {
            showErrorDialog("Invalid destination address!")
            return
        }

        // Then if the amount is valid
        val amt = amountZEC.text.toString()
        val parsedAmt = amt.substring("${DataModel.mainResponseData?.tokenName} ".length, amt.length)
        if (parsedAmt.toDoubleOrNull() == 0.0 || parsedAmt.toDoubleOrNull() == null) {
            showErrorDialog("Invalid amount!")
            return
        }

        // Check if this is more than the maxzspendable
        if (DataModel.mainResponseData?.maxzspendable != null) {
            if (parsedAmt.toDouble() > DataModel.mainResponseData?.maxzspendable!! &&
                parsedAmt.toDouble() <= DataModel.mainResponseData?.maxspendable ?: Double.MAX_VALUE) {

                val alertDialog = AlertDialog.Builder(this@SendActivity)
                alertDialog.setTitle("Send from t-addr?")
                alertDialog.setMessage("${DataModel.mainResponseData?.tokenName} $parsedAmt is more than the balance in " +
                        "your sapling address. This Tx will have to be sent from a transparent address, and will" +
                        " not be private.\n\nAre you absolutely sure?")
                alertDialog.apply {
                    setPositiveButton("Send Anyway") { dialog, id -> doConfirm() }
                    setNegativeButton("Cancel") { dialog, id -> dialog.cancel() }
                }

                alertDialog.create().show()
                return
            }
        }

        // Warning if spending more than total
        if (parsedAmt.toDouble() > DataModel.mainResponseData?.maxspendable ?: Double.MAX_VALUE) {
            showErrorDialog("Can't spend more than ${DataModel.mainResponseData?.tokenName} " +
                    "${DataModel.mainResponseData?.maxspendable} in a single Tx")
            return
        }

        val memo = txtSendMemo.text.toString()
        if (memo.length > 512) {
            showErrorDialog("Memo is too long")
            return
        }

        if (toAddr.startsWith("t") && !memo.isBlank()) {
            showErrorDialog("Can't send a memo to a t-Address")
            return
        }

        doConfirm()
    }

    private fun doConfirm() {
        val toAddr = sendAddress.text.toString()
        val amt = amountZEC.text.toString()
        val parsedAmt = amt.substring("${DataModel.mainResponseData?.tokenName} ".length, amt.length)
        val memo = txtSendMemo.text.toString()

        val intent = Intent(this, TxDetailsActivity::class.java)
        val tx = DataModel.TransactionItem("confirm", 0, parsedAmt, memo,
            toAddr, "", 0)
        intent.putExtra("EXTRA_TXDETAILS", Klaxon().toJsonString(tx))
        startActivityForResult(intent, REQUEST_CONFIRM)
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
                    // Send async, so that we don't mess up the activity flow
                    Handler().post {
                        val tx = Klaxon().parse<DataModel.TransactionItem>(data?.dataString!!)
                        DataModel.sendTx(tx!!)

                        finish()
                    }
                } else {
                    // Cancel
                }
            }
        }
    }

}

package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.activity_tx_details.*
import kotlinx.android.synthetic.main.content_tx_details.*
import java.io.StringReader
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import android.content.Intent
import android.net.Uri
import android.support.constraint.ConstraintLayout


class TxDetailsActivity : AppCompatActivity() {

    private var tx : DataModel.TransactionItem? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tx_details)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        title = "Transaction Details"

        tx = Klaxon().parse(StringReader(intent.getStringExtra("EXTRA_TXDETAILS")))

        if (tx?.type == "send")
            imgTypeColor.setImageResource(R.color.colorAccent)

        if (tx?.type == "confirm") {
            txtType.text = "Confirm Transaction"
            txtDateTime.text = ""
            btnExternal.text = "Confirm and Send"
        } else {
            txtType.text = tx?.type?.capitalize() + if (tx?.confirmations == 0L) " (Unconfirmed Tx)" else ""
            txtDateTime.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                .format(Date((tx?.datetime ?: 0) * 1000))
        }

        txtAddress.text = if (tx?.addr.isNullOrBlank()) "(Shielded Address)" else tx?.addr

        val amt = kotlin.math.abs(tx?.amount?.toDoubleOrNull() ?: 0.0)
        val amtStr = DecimalFormat("#0.0000####").format(amt)

        txtAmtZec.text = "ZEC $amtStr"
        txtAmtUSD.text = "$ " + DecimalFormat("#,##0.00").format(
            (amt) * (DataModel.mainResponseData?.zecprice ?: 0.0))

        if (tx?.memo.isNullOrBlank()) {
            layoutMemo.visibility = ConstraintLayout.GONE
        } else {
            txtMemo.text = tx?.memo
        }

        btnExternal.setOnClickListener { v ->
            if (tx?.type == "confirm") {
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://explorer.zcha.in/transactions/${tx?.txid}")
                )
                startActivity(browserIntent)
            }
        }
    }

    override fun getSupportParentActivityIntent(): Intent? {
        return getParentActivityIntentImpl()
    }

    override fun getParentActivityIntent(): Intent? {
        return getParentActivityIntentImpl()
    }

    private fun getParentActivityIntentImpl(): Intent {
        var i: Intent? = null

        // Here you need to do some logic to determine from which Activity you came.
        // example: you could pass a variable through your Intent extras and check that.
        if (tx?.type == "confirm") {
            i = Intent(this, SendActivity::class.java)
            // set any flags or extras that you need.
            // If you are reusing the previous Activity (i.e. bringing it to the top
            // without re-creating a new instance) set these flags:
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        } else {
            i = Intent(this, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return i
    }
}

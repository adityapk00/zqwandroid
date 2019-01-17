package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
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

        tx = Klaxon().parse(StringReader(intent.getStringExtra("EXTRA_TXDETAILS")))

        if (tx?.type == "send")
            imgTypeColor.setImageResource(R.color.colorAccent)

        txtType.text = tx?.type?.capitalize()
        txtDateTime.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
                                     .format(Date((tx?.datetime ?: 0) * 1000))
        txtAddress.text = if (tx?.addr.isNullOrBlank()) "(Shielded)" else tx?.addr

        val amt = kotlin.math.abs(tx?.amount?.toDoubleOrNull() ?: 0.0)
        val amtStr = DecimalFormat("#0.00000000").format(amt)

        txtAmtZec.text = "ZEC " + amtStr.substring(0, amtStr.length - 4)
        txtAmtZecSmall.text = amtStr.substring(amtStr.length - 4, amtStr.length)
        txtAmtUSD.text = "$ " + DecimalFormat("#0.00").format(
            (amt) * (DataModel.mainResponseData?.zecprice ?: 0.0))

        if (tx?.memo.isNullOrBlank()) {
            layoutMemo.visibility = ConstraintLayout.GONE
        } else {
            txtMemo.text = tx?.memo
        }

        btnExternal.setOnClickListener { v ->
            val browserIntent = Intent(Intent.ACTION_VIEW,
                Uri.parse("https://explorer.zcha.in/transactions/${tx?.txid}"))
            startActivity(browserIntent)
        }
    }

}

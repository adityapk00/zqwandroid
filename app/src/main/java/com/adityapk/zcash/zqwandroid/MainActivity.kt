package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.*
import okio.ByteString
import java.text.DecimalFormat
import android.widget.TextView




class MainActivity : AppCompatActivity(), TransactionItemFragment.OnFragmentInteractionListener , UnconfirmedTxItemFragment.OnFragmentInteractionListener{
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private inner class EchoWebSocketListener : WebSocketListener() {
        private val NORMAL_CLOSURE_STATUS = 1000
        private val TAG = "MainActivity";

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Opened Websocket")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            DataModel.parseResponse(text!!)
            updateUI()
            Log.i(TAG, "Recieving $text")
        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString) {
            Log.i(TAG, "Receiving bytes : " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String?) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            Log.i(TAG,"Closing : $code / $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG,"Failed $t")
        }
    }

    var client: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Zec QT Wallet - Connected"

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab1.setOnClickListener {view ->
            val intent = Intent(this, ReceiveActivity::class.java)
            startActivity(intent)
            closeFABMenu()
        }

        fab2.setOnClickListener {view ->
            val intent = Intent(this, SendActivity::class.java)
            startActivity(intent)
            closeFABMenu()
        }

        fab.setOnClickListener { view ->
            if(!isFABOpen){
                showFABMenu()
            } else {
                closeFABMenu()
            }
        }
        client = OkHttpClient()
        makeAPICalls()

        balanceUSD.setOnClickListener { view ->
            Toast.makeText(applicationContext, "1 ZEC = $${DecimalFormat("#.##").format(DataModel.mainResponseData?.zecprice)}", Toast.LENGTH_LONG).show()
        }

        updateUI()
    }

    private fun makeAPICalls() {
        val request = Request.Builder().url("ws://10.0.2.2:8237").build()
        val listener = EchoWebSocketListener()
        val ws = client?.newWebSocket(request, listener)

        ws?.send(json { obj("command" to "getInfo") }.toJsonString())
        ws?.send(json { obj("command" to "getTransactions")}.toJsonString())
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        runOnUiThread {
            val bal = DataModel.mainResponseData?.balance ?: 0.0
            val zPrice = DataModel.mainResponseData?.zecprice ?: 0.0

            val balText = DecimalFormat("#0.00000000").format(bal)

            balance.text = "ZEC " + balText.substring(0, balText.length - 4)
            balanceSmall.text = balText.substring(balText.length - 4, balText.length)
            balanceUSD.text = "$ " + DecimalFormat("#,##0.00").format(bal * zPrice)

            addPastTransactions(DataModel.transactions)
        }
    }

    private fun addPastTransactions(txns: List<DataModel.TransactionItem>?) {
        txList.removeAllViewsInLayout()

        // If there are no transactions, just return (don't add any headers either)
        if (txns.isNullOrEmpty())
            return

        val addTitle = fun(title: String) {
            // Add the "Past Transactions" TextView
            val tv = TextView(this)
            tv.text = title
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(16, 16, 16, 16)
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            tv.layoutParams = params
            tv.setTypeface(null, Typeface.BOLD)
            txList.addView(tv)
        }

        // Split all the transactions into confirmations = 0 and confirmations > 0
        // Unconfirmed first
        val unconfirmed = txns.filter { t -> t.confirmations == 0L }
        if (unconfirmed.isNotEmpty()) {
            //addTitle("Recent Transactions")
            val fragTx = supportFragmentManager.beginTransaction()

            for (tx in unconfirmed) {
                fragTx.add(
                    txList.id ,
                    UnconfirmedTxItemFragment.newInstance(Klaxon().toJsonString(tx), ""),
                    "tag1"
                )
            }
            fragTx.commit()
        }

        // Add all confirmed transactions
        val confirmed = txns.filter { t -> t.confirmations > 0L }
        if (confirmed.isNotEmpty()) {
            addTitle("Recent Transactions")
            val fragTx = supportFragmentManager.beginTransaction()

            var oddeven = "odd"
            for (tx in confirmed) {
                fragTx.add(
                    txList.id ,
                    TransactionItemFragment.newInstance(Klaxon().toJsonString(tx), oddeven),
                    "tag1"
                )
                oddeven = if (oddeven == "odd") "even" else "odd"
            }
            fragTx.commit()
        }
    }

    private var isFABOpen = false

    private fun showFABMenu() {
        isFABOpen = true
        fab1.animate().translationY(-resources.getDimension(R.dimen.standard_55))
        fab2.animate().translationY(-resources.getDimension(R.dimen.standard_105))
        fab3.animate().translationY(-resources.getDimension(R.dimen.standard_155))
    }

    private fun closeFABMenu() {
        isFABOpen = false
        fab1.animate().translationY(0f)
        fab2.animate().translationY(0f)
        fab3.animate().translationY(0f)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}

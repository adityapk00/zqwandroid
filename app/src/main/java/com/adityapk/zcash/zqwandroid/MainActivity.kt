package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.beust.klaxon.Klaxon
import com.beust.klaxon.json
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.*
import okio.ByteString
import java.net.ConnectException
import java.text.DecimalFormat
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.adityapk.zcash.zqwandroid.DataModel.connStatus
import com.adityapk.zcash.zqwandroid.DataModel.ConnectionStatus


class MainActivity : AppCompatActivity(), TransactionItemFragment.OnFragmentInteractionListener , UnconfirmedTxItemFragment.OnFragmentInteractionListener{
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Zec QT Wallet"

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // When creating, clear all the data first
        setMainStatus("")

        DataModel.init()

        btnConnect.setOnClickListener {
            val intent = Intent(this, QrReaderActivity::class.java)
            intent.putExtra("REQUEST_CODE", QrReaderActivity.REQUEST_CONNDATA)
            startActivityForResult(intent, QrReaderActivity.REQUEST_CONNDATA)
        }

        btnReconnect.setOnClickListener {
            makeConnection()
        }

        swiperefresh.setOnRefreshListener {
            if (connStatus == DataModel.ConnectionStatus.DISCONNECTED) {
                makeConnection()
            }
        }

        txtMainBalanceUSD.setOnClickListener {
            Toast.makeText(applicationContext, "1 ZEC = $${DecimalFormat("#.##")
                .format(DataModel.mainResponseData?.zecprice)}", Toast.LENGTH_LONG).show()
        }

        bottomNav.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.action_send -> {
                    val intent = Intent(this, SendActivity::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.action_bal -> true
                R.id.action_recieve -> {
                    val intent = Intent(this, ReceiveActivity::class.java)
                    startActivity(intent)
                    return@setOnNavigationItemSelectedListener true
                }
                else -> {
                    return@setOnNavigationItemSelectedListener false
                }
            }
        }

        updateUI(false)
    }


    // Attempt a connection to the server. If there is no saved connection, we'll set the connection status
    // to None
    private fun makeConnection(directConn : Boolean = true) {
        val connString = DataModel.getConnString(applicationContext)
        if (connString.isNullOrBlank()) {
            // The user might have just disconnected, so make sure we are disconnected

            DataModel.ws?.close(1000, "disconnected")
            return
        }

        println("MakeConnection")

        // If still connecting, this is a duplicate call, so do nothing but wait.
        if (connStatus == ConnectionStatus.CONNECTING) {
            return
        }

        // If already connected, then refresh data
        if (connStatus == ConnectionStatus.CONNECTED) {
            updateData()
            return
        }

        println("Attempting new connection $connStatus")

        // If direct connection, then connect to the URL in connection string
        if (directConn) {
            // Update status to connecting, so we can update the UI
            connStatus = ConnectionStatus.CONNECTING
            println("Connstatus = connecting")

            val client = OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(connString).build()
            val listener = WebsocketClient(true)

            DataModel.ws = client.newWebSocket(request, listener)

            updateUI(false)
        } else {
            // Connect to the wormhole
            connStatus = ConnectionStatus.CONNECTING

            println("Connstatus = connecting")

            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val request = Request.Builder().url("ws://192.168.5.187:7070").build()
            val listener = WebsocketClient(false)

            DataModel.ws = client.newWebSocket(request, listener)

            updateUI(false)
        }
    }

    private fun updateData() {
        // If there is a connection, get the data model to update itself
        if (connStatus != ConnectionStatus.DISCONNECTED) {
            DataModel.makeAPICalls()
        }
    }

    private fun setMainStatus(status: String) {
        lblBalance.text = ""
        txtMainBalanceUSD.text = ""
        txtMainBalance.text = status
        balanceSmall.text = ""
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(updateTxns: Boolean) {
        runOnUiThread {
            Log.i(TAG, "Updating UI $updateTxns")

            bottomNav.menu.findItem(R.id.action_bal).isChecked = true
            when (connStatus) {
                ConnectionStatus.DISCONNECTED -> {
                    setMainStatus("No Connection")

                    scrollViewTxns.visibility = ScrollView.GONE
                    layoutConnect.visibility = ConstraintLayout.VISIBLE
                    swiperefresh.isRefreshing = false

                    // Hide the reconnect button if there is no connection string
                    if (DataModel.getConnString(ZQWApp.appContext!!).isNullOrBlank() ||
                        DataModel.getSecret() == null) {
                        btnReconnect.visibility = Button.GONE
                        lblConnectionOr.visibility = TextView.GONE
                    } else {
                        btnReconnect.visibility = Button.VISIBLE
                        lblConnectionOr.visibility = TextView.VISIBLE
                    }

                    // Disable the send and recieve buttons
                    bottomNav.menu.findItem(R.id.action_recieve).isEnabled = false
                    bottomNav.menu.findItem(R.id.action_send).isEnabled = false

                    if (updateTxns) {
                        Handler().post {
                            run {
                                addPastTransactions(DataModel.transactions)
                            }
                        }
                    }
                }
                ConnectionStatus.CONNECTING -> {
                    setMainStatus("Connecting...")
                    scrollViewTxns.visibility = ScrollView.GONE
                    layoutConnect.visibility = ConstraintLayout.GONE
                    swiperefresh.isRefreshing = true

                    // Disable the send and recieve buttons
                    bottomNav.menu.findItem(R.id.action_recieve).isEnabled = false
                    bottomNav.menu.findItem(R.id.action_send).isEnabled = false
                }
                ConnectionStatus.CONNECTED -> {
                    scrollViewTxns.visibility = ScrollView.VISIBLE
                    layoutConnect.visibility = ConstraintLayout.GONE

                    if (DataModel.mainResponseData == null) {
                        setMainStatus("Loading...")
                    } else {
                        val bal = DataModel.mainResponseData?.balance ?: 0.0
                        val zPrice = DataModel.mainResponseData?.zecprice ?: 0.0

                        val balText = DecimalFormat("#0.00000000").format(bal)

                        lblBalance.text = "Balance"
                        txtMainBalance.text = "${DataModel.mainResponseData?.tokenName} " + balText.substring(0, balText.length - 4)
                        balanceSmall.text = balText.substring(balText.length - 4, balText.length)
                        txtMainBalanceUSD.text = "$ " + DecimalFormat("#,##0.00").format(bal * zPrice)

                        // Enable the send and recieve buttons
                        bottomNav.menu.findItem(R.id.action_recieve).isEnabled = true
                        bottomNav.menu.findItem(R.id.action_send).isEnabled = true
                    }

                    if (updateTxns) {
                        Handler().post {
                            run {
                                addPastTransactions(DataModel.transactions)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addPastTransactions(txns: List<DataModel.TransactionItem>?) {
        runOnUiThread {
            val fragTx = supportFragmentManager.beginTransaction()

            for (fr in supportFragmentManager.fragments) {
                fragTx.remove(fr)
            }

            // If there are no transactions, make sure to commit the Tx, so existing items are removed, and just return
            if (txns.isNullOrEmpty()) {
                fragTx.commitAllowingStateLoss()

                swiperefresh.isRefreshing = false
                return@runOnUiThread
            }

            // Split all the transactions into confirmations = 0 and confirmations > 0
            // Unconfirmed first
            val unconfirmed = txns.filter { t -> t.confirmations == 0L }
            if (unconfirmed.isNotEmpty()) {
                for (tx in unconfirmed) {
                    fragTx.add(
                        txList.id ,
                        UnconfirmedTxItemFragment.newInstance(Klaxon().toJsonString(tx), ""),
                        "tag1"
                    )
                }
            }

            // Add all confirmed transactions
            val confirmed = txns.filter { t -> t.confirmations > 0L }
            if (confirmed.isNotEmpty()) {
                var oddeven = "odd"
                for (tx in confirmed) {
                    fragTx.add(
                        txList.id,
                        TransactionItemFragment.newInstance(Klaxon().toJsonString(tx), oddeven),
                        "tag1"
                    )
                    oddeven = if (oddeven == "odd") "even" else "odd"
                }
            }
            fragTx.commitAllowingStateLoss()

            swiperefresh.isRefreshing = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_refresh -> {
                swiperefresh.isRefreshing = true
                updateData()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        Handler().post {
            Log.i(TAG,"OnResume for mainactivity")
            makeConnection()
        }

        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            QrReaderActivity.REQUEST_CONNDATA -> {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Main Activity got result for QrCode: ${data?.dataString}")

                    // Check to make sure that the result is an actual address
                    if (!(data?.dataString ?: "").startsWith("ws")) {
                        Toast.makeText(applicationContext,
                            "${data?.dataString} is not a valid connection string", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val conComponents = data?.dataString?.split(",")
                    if (conComponents?.size != 2) {
                        Toast.makeText(applicationContext,
                            "${data?.dataString} is not a valid connection string", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val conString = conComponents[0]
                    val secretHex = conComponents[1]

                    DataModel.setSecretHex(secretHex)
                    DataModel.setConnString(conString, applicationContext)

                    makeConnection()
                }
            }
        }
    }

    private fun disconnected(reason: String? = null) {
        Log.i(TAG, "Disconnected")

        connStatus = ConnectionStatus.DISCONNECTED
        println("Connstatus = Disconnected")

        DataModel.clear()
        updateUI(true)

        if (!reason.isNullOrEmpty()) {
            Snackbar.make(layoutConnect, "Server says: ${reason}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun clearConnection(msg : String? ) {
        Log.i(TAG, "Clearing connection")

        DataModel.ws?.close(1000, msg ?: "Forcibly closing connection")

        // If the server returned an displayMsg, we need to clear out the connection,
        // forcing a reconnection
        DataModel.setConnString(null, applicationContext)

        disconnected()
    }

    private inner class WebsocketClient (directConn: Boolean) : WebSocketListener() {
        val m_directConn = directConn

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Opened Websocket")
            connStatus = ConnectionStatus.CONNECTED
            println("Connstatus = connected")

            // If this is a connection to wormhole, we have to register ourselves
            if (!m_directConn) {
                if (!DataModel.getWormholeCode().isNullOrBlank())
                    webSocket.send( json { obj( "register" to DataModel.getWormholeCode()) }.toJsonString())
            }

            updateData()
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(TAG, "Receiving $text")

            val r = DataModel.parseResponse(text!!)

            if (r.displayMsg != null) {
                Snackbar.make(layoutConnect, "${r.displayMsg}", Snackbar.LENGTH_LONG).show()
            }
            if (r.doDisconnect) {
                clearConnection(r.displayMsg)
            }
            updateUI(r.updateTxns)
        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString) {
            Log.i(TAG, "Receiving bytes : " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String?) {
            webSocket.close(1000, null)
            connStatus = ConnectionStatus.DISCONNECTED
            println("Connstatus = disconnected")

            Log.i(TAG,"Closing : $code / $reason")
            disconnected(reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG,"Failed $t")

            if (t is ConnectException) {
                Snackbar.make(layoutConnect, t.localizedMessage, Snackbar.LENGTH_LONG).show()
            }

            disconnected()

            // If this was a direct connection and there was a failure to connect, retry connecting
            // without the direct connection (i.e., through wormhole)
            if (m_directConn) {
                makeConnection(false)
            }

        }
    }

    private val TAG = "MainActivity"
}

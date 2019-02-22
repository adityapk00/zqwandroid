package com.adityapk.zcash.zqwandroid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.adityapk.zcash.zqwandroid.DataModel.ConnectionStatus
import com.adityapk.zcash.zqwandroid.DataModel.connStatus
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.DecimalFormat


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
            ConnectionManager.refreshAllData()
        }

        swiperefresh.setOnRefreshListener {
            ConnectionManager.refreshAllData()
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
                    } else {
                        swiperefresh.isRefreshing = false
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
                ConnectionManager.refreshAllData()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // We've received a signal
            when(intent.getStringExtra("action")) {
                "refresh" -> {
                    swiperefresh.isRefreshing = !intent.getBooleanExtra("finished", true)
                }
                "newdata" -> {
                    val updateTxns = intent.getBooleanExtra("updateTxns", false)
                    updateUI(updateTxns)
                }
                "error" -> {
                    val msg = intent.getStringExtra("msg")

                    if (!msg.isNullOrEmpty()) {
                        Snackbar.make(layoutConnect, msg, Snackbar.LENGTH_LONG).show()
                    }

                    // Also check if we need to disconnect
                    if (intent.getBooleanExtra("doDisconnect", false)) {
                        disconnected()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, IntentFilter(ConnectionManager.DATA_SIGNAL))

        // On resuming, refresh all data
        ConnectionManager.refreshAllData()
    }

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        super.onDestroy()
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
                    if (conComponents?.size ?: 0 < 2 || conComponents?.size ?: 0 > 3) {
                        Toast.makeText(applicationContext,
                            "${data?.dataString} is not a valid connection string", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val conString = conComponents!![0]
                    val secretHex = conComponents[1]
                    val allowInternetConnections = if (conComponents.size == 3) conComponents[2] == "1" else false

                    DataModel.setSecretHex(secretHex)
                    DataModel.setConnString(conString, applicationContext)
                    DataModel.setAllowInternet(allowInternetConnections)

                    ConnectionManager.refreshAllData()
                }
            }
        }
    }

    private fun disconnected() {
        Log.i(TAG, "Disconnected")

        connStatus = ConnectionStatus.DISCONNECTED
        println("Connstatus = Disconnected")

        DataModel.clear()
        swiperefresh.isRefreshing = false
        updateUI(true)
    }


    private val TAG = "MainActivity"
}

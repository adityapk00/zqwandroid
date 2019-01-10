package com.adityapk.zcash.zec_qt_wallet_android

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.view.Window

import kotlinx.android.synthetic.main.activity_main.*
import android.support.v4.view.ViewCompat.animate
import android.R.attr.translationY



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Total Balance"

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val fab1 = findViewById<FloatingActionButton>(R.id.fab1);
        val fab2 = findViewById<FloatingActionButton>(R.id.fab2);
        val fab3 = findViewById<FloatingActionButton>(R.id.fab3);

        fab1.setOnClickListener {view ->
            val intent = Intent(this, ReceiveActivity::class.java)
            startActivity(intent)
        }

        fab.setOnClickListener { view ->
            if(!isFABOpen){
                showFABMenu();
            } else {
                closeFABMenu();
            }
        }
    }


    var isFABOpen = false

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

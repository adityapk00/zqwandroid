package com.adityapk.zcash.zqwandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import com.google.zxing.WriterException
import kotlinx.android.synthetic.main.activity_receive.*
import kotlinx.android.synthetic.main.content_receive.*


class ReceiveActivity : AppCompatActivity() {

    private var addr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Receive"

        setContentView(R.layout.activity_receive)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tabAddressType.setOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(p0: TabLayout.Tab?) {
                if (p0?.text == "zAddr") {
                    setZAddr()
                } else {
                    setTAddr()
                }
            }

        })

        setZAddr()
    }

    fun setAddr() {
        val qrgEncoder = QRGEncoder(addr, null, QRGContents.Type.TEXT, 300)
        try {
            // Getting QR-Code as Bitmap
            val bitmap = qrgEncoder.encodeAsBitmap()
            // Setting Bitmap to ImageView
            val qrImage = findViewById<ImageView>(R.id.imageView)
            qrImage.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Log.w("receive", e.toString())
        }

        if (addr.isNullOrBlank())
            addr = "(no address)"

        val addrTxt = findViewById<TextView>(R.id.addressTxt)

        val numsplits = if (addr!!.length > 48) 8 else 4
        val size = addr!!.length / numsplits

        var splitText = ""
        for (i in 0..(numsplits-1)) {
            splitText += addr?.substring(i * size, i * size + size)
            splitText += if (i % 2 == 0) " " else "\n"
        }
        addrTxt.text = splitText

        addrTxt.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("zcash address", addr)
            clipboard.primaryClip = clip
            Toast.makeText(applicationContext, "Copied address to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    fun setTAddr() {
        addr = DataModel.mainResponseData?.tAddress ?: ""
        txtRcvAddrTitle.text = "Your zcash transparent address"
        setAddr()
    }

    fun setZAddr() {
        addr = DataModel.mainResponseData?.saplingAddress ?: ""
        txtRcvAddrTitle.text = "Your zcash sapling address"
        setAddr()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recieve, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_share -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, addr)
                    type = "text/plain"
                }
                startActivity(sendIntent)

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

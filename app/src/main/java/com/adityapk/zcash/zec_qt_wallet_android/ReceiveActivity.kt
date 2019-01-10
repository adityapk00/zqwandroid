package com.adityapk.zcash.zec_qt_wallet_android

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_receive.*
import com.google.zxing.WriterException
import android.R.attr.bitmap
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import kotlinx.android.synthetic.main.content_receive.*
import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.support.v4.content.ContextCompat.getSystemService




class ReceiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Receive"

        setContentView(R.layout.activity_receive)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addr = "zs1gv64eu0v2wx7raxqxlmj354y9ycznwaau9kduljzczxztvs4qcl00kn2sjxtejvrxnkucw5xx9u"

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

        val addrTxt = findViewById<TextView>(R.id.addressTxt)
        var splitText = ""
        val size = addr.length / 8
        for (i in 0..7) {
            splitText += addr.substring(i * size, i * size + size)
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

}

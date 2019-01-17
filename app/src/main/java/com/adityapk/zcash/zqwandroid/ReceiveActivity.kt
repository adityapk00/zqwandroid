package com.adityapk.zcash.zqwandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import com.google.zxing.WriterException
import kotlinx.android.synthetic.main.activity_receive.*


class ReceiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = "Receive"

        setContentView(R.layout.activity_receive)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addr = DataModel.mainResponseData?.saplingAddress ?: "no address"

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

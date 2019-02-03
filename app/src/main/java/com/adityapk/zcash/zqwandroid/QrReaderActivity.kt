package com.adityapk.zcash.zqwandroid

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_qr_reader.*
import java.io.IOException
import android.R.string.cancel
import android.app.AlertDialog
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText



class QrReaderActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_ADDRESS = 1
        const val REQUEST_CONNDATA = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_reader)

        title = "Scan QR Code"

        val code = intent.getIntExtra("REQUEST_CODE", 0)
        if (code == REQUEST_ADDRESS)
            txtQrCodeHelp.text = ""

        lblErrorMsg.text = ""

        setupCamera()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            50 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Do what if user refuses permission? Go back?
                } else {
                    recreate()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_qrcodereader, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_manual_input -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Paste the code here manually")

                // Set up the input
                val input = EditText(this)
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.inputType = InputType.TYPE_CLASS_TEXT
                builder.setView(input)

                // Set up the buttons
                builder.setPositiveButton("OK") { dialog, which ->
                    run {
                        val txt = input.text.toString()
                        processText(txt)
                    }
                }
                builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

                builder.create().show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupCamera() {
        val cameraView = findViewById<SurfaceView>(R.id.camera_view)

        val barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        val cameraSource = CameraSource.Builder(this, barcodeDetector)
                                .setAutoFocusEnabled(true)
                                .setRequestedPreviewSize(640, 480)
                                .build()


        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@QrReaderActivity, arrayOf(android.Manifest.permission.CAMERA), 50)
                    } else {
                        cameraSource.start(cameraView.holder)

                        val w = cameraView.width
                        val h = cameraView.height
                        val scale = cameraSource.previewSize.width.toDouble() / cameraSource.previewSize.height.toDouble()

                        val scaleWidth = (h.toDouble() / scale).toInt()

                        cameraView.layout((w - scaleWidth)/2, 0, scaleWidth , h)
                        println("Preview size: ${cameraSource.previewSize}")
                    }
                } catch (ie: IOException) {
                    Log.e("CAMERA SOURCE", ie.message)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        btnQrCodeCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() != 0) {
                    runOnUiThread {
                        val barcodeInfo = barcodes.valueAt(0).displayValue
                        processText(barcodeInfo)
                    }
                }
            }
        })
    }

    private fun processText(barcodeInfo: String) {
        val code = intent.getIntExtra("REQUEST_CODE", 0)

        // See if this the data is of the right format
        if (code == REQUEST_CONNDATA && !barcodeInfo.startsWith("ws")) {
            Log.i(TAG, "Not a connection")
            var err = barcodeInfo
            if (err.length > 48) {
                err = err.substring(0, 22) + "...." + err.substring(err.length - 22, err.length)
            }
            lblErrorMsg.text = "\"$err\" is not a connection string"
            return
        }

        if (code == REQUEST_ADDRESS && !DataModel.isValidAddress(StringBuilder(barcodeInfo ?: "").toString())) {
            Log.i(TAG, "Not an address")
            var err = barcodeInfo
            if (err.length > 48) {
                err = err.substring(0, 22) + "...." + err.substring(err.length - 22, err.length)
            }
            lblErrorMsg.text = "\"$err\" is not a valid address"
            return
        }

        // The data seems valid, so return it.
        val data = Intent()
        data.data = Uri.parse(barcodeInfo)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private val TAG = "QrReader"
}

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
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_qr_reader.*
import java.io.IOException

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

        if (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 50)
        } else {
            setupCamera()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            50 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Do what if user refuses permission? Go back?
                } else {
                    setupCamera()
                }
            }
        }
    }

    fun setupCamera() {
        val cameraView = findViewById<SurfaceView>(R.id.camera_view)

        val barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        val cameraSource = CameraSource.Builder(this, barcodeDetector)
                                .setAutoFocusEnabled(true)
                                .setRequestedPreviewSize(640, 480)
                                .build()

        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    cameraSource.start(cameraView.getHolder())
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

                val code = intent.getIntExtra("REQUEST_CODE", 0)

                if (barcodes.size() != 0) {
                    runOnUiThread {
                        val barcodeInfo = barcodes.valueAt(0).displayValue

                        // See if this the data is of the right format
                        if (code == REQUEST_CONNDATA && !barcodeInfo.startsWith("ws")) {
                            Log.i(TAG, "Not a connection")
                            return@runOnUiThread
                        }

                        if (code == REQUEST_ADDRESS && !DataModel.isValidAddress(StringBuilder(barcodeInfo ?: "").toString())) {
                            Log.i(TAG, "Not an address")
                            return@runOnUiThread
                        }

                        // The data seems valid, so return it.
                        val data = Intent()
                        data.data = Uri.parse(barcodes.valueAt(0).displayValue)
                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }

                }
            }
        })
    }

    private val TAG = "QrReader"
}

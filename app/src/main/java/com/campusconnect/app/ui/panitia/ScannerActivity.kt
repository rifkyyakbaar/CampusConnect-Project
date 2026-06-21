package com.campusconnect.app.ui.panitia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Ticket
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class ScannerActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: DecoratedBarcodeView
    private lateinit var btnBackScanner: ImageView
    private lateinit var tvScanInstruction: TextView

    private var eventId = ""
    private var eventName = ""

    // Mencegah dua tiket diproses bersamaan saat kamera mendeteksi
    // QR berkali-kali dalam satu frame burst.
    private var isProcessing = false

    private val requestCameraPermission =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Izin kamera diperlukan untuk scan QR Code",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        eventId = intent.getStringExtra("eventId").orEmpty()
        eventName = intent.getStringExtra("eventName").orEmpty()

        initViews()
        ensureCameraPermissionThenScan()
    }

    private fun initViews() {

        barcodeScanner = findViewById(R.id.barcodeScanner)
        btnBackScanner = findViewById(R.id.btnBackScanner)
        tvScanInstruction = findViewById(R.id.tvScanInstruction)

        btnBackScanner.setOnClickListener {
            finish()
        }
    }

    private fun ensureCameraPermissionThenScan() {

        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startScanning()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanning() {

        barcodeScanner.decodeContinuous(object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult?) {

                val scannedText = result?.text ?: return

                if (isProcessing) return
                isProcessing = true

                barcodeScanner.pause()

                handleScannedTicket(scannedText)
            }

            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                // tidak perlu ditangani
            }
        })
    }

    private fun handleScannedTicket(ticketId: String) {

        SupabaseRepository.getTicketById(this, ticketId) { result ->

            result.onSuccess { ticket ->

                validateTicket(ticket)

            }.onFailure {

                showResultDialog(
                    title = "QR Tidak Valid",
                    message = "Tiket tidak ditemukan di sistem.",
                    isError = true
                )
            }
        }
    }

    private fun validateTicket(ticket: Ticket) {

        // 1) Tiket harus milik event yang sedang dibuka panitia.
        //    QR Seminar A tidak boleh dipakai untuk check-in Seminar B.
        if (ticket.eventId != eventId) {

            showResultDialog(
                title = "QR Tidak Valid",
                message = "Tiket ini bukan untuk event \"$eventName\".",
                isError = true
            )

            return
        }

        // 2) Cek status tiket.
        when (ticket.status) {

            "CONFIRMED" -> {
                markAsUsed(ticket)
            }

            "USED" -> {
                showResultDialog(
                    title = "Tiket Sudah Digunakan",
                    message = "${ticket.attendeeName} sudah pernah check-in sebelumnya.",
                    isError = true
                )
            }

            "REJECTED" -> {
                showResultDialog(
                    title = "Pembayaran Ditolak",
                    message = "Tiket ini tidak valid karena pembayaran ditolak panitia.",
                    isError = true
                )
            }

            "PENDING" -> {
                showResultDialog(
                    title = "Pembayaran Belum Diverifikasi",
                    message = "Tiket ${ticket.attendeeName} masih menunggu verifikasi pembayaran.",
                    isError = true
                )
            }

            else -> {
                showResultDialog(
                    title = "Status Tidak Dikenali",
                    message = "Status tiket: ${ticket.status}",
                    isError = true
                )
            }
        }
    }

    private fun markAsUsed(ticket: Ticket) {

        SupabaseRepository.markTicketUsed(this, ticket.ticketId) { result ->

            result.onSuccess {

                showResultDialog(
                    title = "Tiket Berhasil Digunakan",
                    message = "${ticket.attendeeName}\n${ticket.attendeeRole}",
                    isError = false
                )

            }.onFailure {

                showResultDialog(
                    title = "Gagal Memproses Tiket",
                    message = it.message ?: "Terjadi kesalahan saat memperbarui status tiket.",
                    isError = true
                )
            }
        }
    }

    private fun showResultDialog(title: String, message: String, isError: Boolean) {

        runOnUiThread {

            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Scan Lagi") { dialog, _ ->
                    dialog.dismiss()
                    isProcessing = false
                    barcodeScanner.resume()
                }
                .setNegativeButton("Selesai") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::barcodeScanner.isInitialized && !isProcessing) {
            barcodeScanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::barcodeScanner.isInitialized) {
            barcodeScanner.pause()
        }
    }
}
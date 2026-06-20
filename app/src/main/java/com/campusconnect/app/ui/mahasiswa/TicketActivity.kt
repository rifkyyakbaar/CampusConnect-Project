package com.campusconnect.app.ui.mahasiswa

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class TicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)

        // Tombol kembali
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Ambil data dari Intent
        val category = intent.getStringExtra("category") ?: ""
        val eventName = intent.getStringExtra("eventName") ?: ""
        val eventDate = intent.getStringExtra("eventDate") ?: ""
        val location = intent.getStringExtra("eventLocation") ?: ""
        val ticketId = intent.getStringExtra("ticketId") ?: ""
        val attendeeName = intent.getStringExtra("attendeeName") ?: ""
        val attendeeRole = intent.getStringExtra("attendeeRole") ?: ""
        val status = intent.getStringExtra("status") ?: ""

        // Hubungkan dengan TextView di activity_ticket.xml
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvTicketEventName = findViewById<TextView>(R.id.tvTicketEventName)
        val tvTicketName = findViewById<TextView>(R.id.tvTicketName)
        val tvTicketRole = findViewById<TextView>(R.id.tvTicketRole)
        val tvTicketDate = findViewById<TextView>(R.id.tvTicketDate)
        val tvTicketVenue = findViewById<TextView>(R.id.tvTicketVenue)
        val tvTicketID = findViewById<TextView>(R.id.tvTicketID)
        val ivQRCode = findViewById<ImageView>(R.id.ivQRCode)
        val tvTicketStatus = findViewById<TextView>(R.id.tvTicketStatus)

        // Isi data tiket
        tvCategory.text = category.uppercase()
        tvTicketName.text = attendeeName
        tvTicketRole.text = attendeeRole
        tvTicketEventName.text = eventName
        tvTicketDate.text = eventDate
        tvTicketVenue.text = location
        tvTicketID.text = "ID: $ticketId"
        tvTicketStatus.text = status

        val qrBitmap = generateQRCode(ticketId)

        ivQRCode.setImageBitmap(qrBitmap)

        btnBack.setOnClickListener {
            finish()
        }

        setupBottomNavigation()

    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_ticket

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    startActivity(
                        Intent(this, HomeMahasiswaActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_ticket -> true

                R.id.nav_history -> {
                    startActivity(
                        Intent(this, HistoryActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun generateQRCode(text: String): Bitmap {

        val size = 800

        val bitMatrix: BitMatrix =
            MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until size) {
            for (y in 0 until size) {

                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y])
                        android.graphics.Color.BLACK
                    else
                        android.graphics.Color.WHITE
                )
            }
        }

        return bitmap
    }
}
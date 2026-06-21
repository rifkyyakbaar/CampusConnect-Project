package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

class TicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        val category     = intent.getStringExtra("category").orEmpty()
        val eventName    = intent.getStringExtra("eventName").orEmpty()
        val eventDate    = intent.getStringExtra("eventDate").orEmpty()
        val location     = intent.getStringExtra("eventLocation").orEmpty()
        val ticketId     = intent.getStringExtra("ticketId").orEmpty()
        val eventId      = intent.getStringExtra("eventId").orEmpty()
        val attendeeName = intent.getStringExtra("attendeeName").orEmpty()
        val attendeeRole = intent.getStringExtra("attendeeRole").orEmpty()
        val status       = intent.getStringExtra("status").orEmpty()

        findViewById<TextView>(R.id.tvCategory).text       = category.uppercase()
        findViewById<TextView>(R.id.tvTicketEventName).text = eventName
        findViewById<TextView>(R.id.tvTicketName).text     = attendeeName
        findViewById<TextView>(R.id.tvTicketRole).text     = attendeeRole
        findViewById<TextView>(R.id.tvTicketDate).text     = eventDate
        findViewById<TextView>(R.id.tvTicketVenue).text    = location
        findViewById<TextView>(R.id.tvTicketID).text       = "ID: $ticketId"
        findViewById<TextView>(R.id.tvTicketStatus).text   = status
        findViewById<ImageView>(R.id.ivQRCode).setImageBitmap(generateQRCode(ticketId))

        // Tombol Review — hanya muncul jika tiket USED
        val btnReview = findViewById<Button>(R.id.btnReviewEvent)
        if (status == "USED") {
            // Cek apakah sudah direview
            SupabaseRepository.hasReviewed(this, ticketId) { result ->
                val alreadyReviewed = result.getOrDefault(false)
                if (alreadyReviewed) {
                    btnReview.text = "Sudah Direview ✓"
                    btnReview.isEnabled = false
                    btnReview.visibility = View.VISIBLE
                } else {
                    btnReview.visibility = View.VISIBLE
                    btnReview.setOnClickListener {
                        startActivity(
                            Intent(this, ReviewActivity::class.java).apply {
                                putExtra("ticketId", ticketId)
                                putExtra("eventId", eventId)
                            }
                        )
                    }
                }
            }
        } else {
            btnReview.visibility = View.GONE
        }

        btnBack.setOnClickListener { finish() }
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Refresh status review setiap kembali ke halaman ini
        val ticketId = intent.getStringExtra("ticketId").orEmpty()
        val status   = intent.getStringExtra("status").orEmpty()
        val btnReview = findViewById<Button>(R.id.btnReviewEvent)
        if (status == "USED") {
            SupabaseRepository.hasReviewed(this, ticketId) { result ->
                val alreadyReviewed = result.getOrDefault(false)
                btnReview.visibility = View.VISIBLE
                if (alreadyReviewed) {
                    btnReview.text = "Sudah Direview ✓"
                    btnReview.isEnabled = false
                } else {
                    btnReview.text = "Review Event"
                    btnReview.isEnabled = true
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_ticket
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeMahasiswaActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_ticket -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0); true
                }
                else -> false
            }
        }
    }

    private fun generateQRCode(text: String): Bitmap {
        val size = 800
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size)
            for (y in 0 until size)
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        return bitmap
    }
}

package com.campusconnect.app.ui.mahasiswa

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
        val eventName = intent.getStringExtra("eventName") ?: ""
        val eventDate = intent.getStringExtra("eventDate") ?: ""
        val location = intent.getStringExtra("eventLocation") ?: ""
        val ticketId = intent.getStringExtra("ticketId") ?: ""

        // Hubungkan dengan TextView di activity_ticket.xml
        val tvTicketEventName = findViewById<TextView>(R.id.tvTicketEventName)
        val tvTicketDate = findViewById<TextView>(R.id.tvTicketDate)
        val tvTicketVenue = findViewById<TextView>(R.id.tvTicketVenue)
        val tvTicketID = findViewById<TextView>(R.id.tvTicketID)

        // Isi data tiket
        tvTicketEventName.text = eventName
        tvTicketDate.text = eventDate
        tvTicketVenue.text = location
        tvTicketID.text = "ID: $ticketId"

        btnBack.setOnClickListener {
            startActivity(
                Intent(this, ManageTicketActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            overridePendingTransition(0, 0)
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
}
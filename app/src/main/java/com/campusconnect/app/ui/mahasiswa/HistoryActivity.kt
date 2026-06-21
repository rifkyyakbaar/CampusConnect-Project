package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.TicketAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyHistory: LinearLayout
    private lateinit var adapter: TicketAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initViews()
        setupRecyclerView()
        loadHistory()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun initViews() {
        rvHistory           = findViewById(R.id.rvHistory)
        layoutEmptyHistory  = findViewById(R.id.layoutEmptyHistory)
    }

    private fun setupRecyclerView() {
        adapter = TicketAdapter(
            onClick = { ticket ->
                // Buka detail tiket
                startActivity(Intent(this, TicketActivity::class.java).apply {
                    putExtra("ticketId",      ticket.ticketId)
                    putExtra("eventId",       ticket.eventId)
                    putExtra("eventName",     ticket.eventName)
                    putExtra("eventDate",     ticket.eventDate)
                    putExtra("eventLocation", ticket.eventLocation)
                    putExtra("category",      ticket.category)
                    putExtra("status",        ticket.status)
                    putExtra("attendeeName",  ticket.attendeeName)
                    putExtra("attendeeRole",  ticket.attendeeRole)
                })
            },
            onReview = { ticket ->
                // Langsung ke halaman review dari item history
                startActivity(Intent(this, ReviewActivity::class.java).apply {
                    putExtra("ticketId", ticket.ticketId)
                    putExtra("eventId",  ticket.eventId)
                })
            }
        )
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = adapter
    }

    private fun loadHistory() {
        SupabaseRepository.loadHistoryTickets(this) { result ->
            result.onSuccess { tickets ->
                adapter.submitList(tickets)
                if (tickets.isEmpty()) {
                    layoutEmptyHistory.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    layoutEmptyHistory.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                }
            }
            result.onFailure {
                layoutEmptyHistory.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_history
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeMahasiswaActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_ticket -> {
                    startActivity(Intent(this, ManageTicketActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_history -> true
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
}

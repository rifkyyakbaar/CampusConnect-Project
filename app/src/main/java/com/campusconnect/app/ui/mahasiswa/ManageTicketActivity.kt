package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusconnect.app.R
import com.campusconnect.app.adapter.TicketAdapter
import com.campusconnect.app.databinding.ActivityManageTicketBinding
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity

class ManageTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTicketBinding
    private lateinit var adapter: TicketAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadUserTickets()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        // ManageTicket hanya untuk tiket CONFIRMED — tidak perlu tombol review
        // onReview dibiarkan null (default) supaya tombol review tidak muncul di sini
        adapter = TicketAdapter(
            onClick = { ticket ->
                startActivity(
                    Intent(this, TicketActivity::class.java).apply {
                        putExtra("ticketId",      ticket.ticketId)
                        putExtra("eventId",       ticket.eventId)
                        putExtra("eventName",     ticket.eventName)
                        putExtra("eventDate",     ticket.eventDate)
                        putExtra("eventLocation", ticket.eventLocation)
                        putExtra("category",      ticket.category)
                        putExtra("status",        ticket.status)
                        putExtra("attendeeName",  ticket.attendeeName)
                        putExtra("attendeeRole",  ticket.attendeeRole)
                    }
                )
            }
            // onReview tidak diisi → default null → tombol review tidak muncul
        )

        binding.rvTickets.layoutManager = LinearLayoutManager(this)
        binding.rvTickets.adapter = adapter
    }

    private fun loadUserTickets() {
        SupabaseRepository.loadUserTickets(this) { result ->
            result.onSuccess { ticketList ->
                adapter.submitList(ticketList)
                if (ticketList.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvTickets.visibility   = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvTickets.visibility   = View.VISIBLE
                }
            }
            result.onFailure {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvTickets.visibility   = View.GONE
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_ticket
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeMahasiswaActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_ticket  -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0, 0); true
                }
                else -> false
            }
        }
    }
}
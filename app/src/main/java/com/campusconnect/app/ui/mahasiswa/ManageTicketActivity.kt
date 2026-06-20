package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusconnect.app.R
import com.campusconnect.app.databinding.ActivityManageTicketBinding
import com.campusconnect.app.model.Ticket
import com.campusconnect.app.ui.profile.ProfileActivity

class ManageTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTicketBinding
    private lateinit var adapter: TicketAdapter

    private val tickets = mutableListOf<Ticket>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManageTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadDummyTickets()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {

        adapter = TicketAdapter(tickets) { ticket ->

            val intent = Intent(this, TicketActivity::class.java)

            intent.putExtra("ticketId", ticket.ticketId)
            intent.putExtra("eventId", ticket.eventId)
            intent.putExtra("eventName", ticket.eventName)
            intent.putExtra("eventDate", ticket.eventDate)
            intent.putExtra("eventLocation", ticket.eventLocation)
            intent.putExtra("category", ticket.category)
            intent.putExtra("status", ticket.status)

            startActivity(intent)
        }

        binding.rvTickets.layoutManager =
            LinearLayoutManager(this)

        binding.rvTickets.adapter = adapter
    }

    private fun loadDummyTickets() {

        tickets.add(
            Ticket(
                "CC-8924-XYZ",
                "1",
                "Seminar Nasional AI",
                "Seminar",
                "20 Juni 2026",
                "Aula FT Unram",
                "Confirmed"
            )
        )

        tickets.add(
            Ticket(
                "CC-6543-ABC",
                "2",
                "Workshop Android",
                "Workshop",
                "25 Juni 2026",
                "Gedung Kuliah Bersama",
                "Confirmed"
            )
        )

        adapter.notifyDataSetChanged()

        if (tickets.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvTickets.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvTickets.visibility = View.VISIBLE
        }
    }

    private fun setupBottomNavigation() {

        binding.bottomNavigation.selectedItemId = R.id.nav_ticket

        binding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.nav_home -> {

                    startActivity(
                        Intent(this, HomeMahasiswaActivity::class.java)
                    )

                    overridePendingTransition(0,0)
                    true
                }

                R.id.nav_ticket -> true

                R.id.nav_history -> {

                    startActivity(
                        Intent(this, HistoryActivity::class.java)
                    )

                    overridePendingTransition(0,0)
                    true
                }

                R.id.nav_profile -> {

                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )

                    overridePendingTransition(0,0)
                    true
                }

                else -> false
            }
        }
    }
}
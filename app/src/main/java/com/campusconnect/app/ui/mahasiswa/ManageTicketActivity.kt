package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.campusconnect.app.R
import com.campusconnect.app.databinding.ActivityManageTicketBinding
import com.campusconnect.app.model.Ticket
import com.campusconnect.app.ui.profile.ProfileActivity
import com.campusconnect.app.utils.setBlinkOnClick

class ManageTicketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageTicketBinding
    private lateinit var adapter: TicketAdapter

    private val allTickets = mutableListOf<Ticket>()
    private var filteredTickets = mutableListOf<Ticket>()
    private var isDateAscending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTicketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadDummyTickets()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = TicketAdapter { ticket ->
            val intent = Intent(this, TicketActivity::class.java).apply {
                putExtra("ticketId", ticket.ticketId)
                putExtra("eventId", ticket.eventId)
                putExtra("eventName", ticket.eventName)
                putExtra("eventDate", ticket.eventDate)
                putExtra("eventLocation", ticket.eventLocation)
                putExtra("category", ticket.category)
                putExtra("status", ticket.status)
            }
            startActivity(intent)
        }
        binding.rvTickets.layoutManager = LinearLayoutManager(this)
        binding.rvTickets.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSortTicket.setBlinkOnClick {
            isDateAscending = !isDateAscending
            sortTickets()
        }

        binding.etSearchTicket.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTickets(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterTickets(query: String) {
        filteredTickets = if (query.isEmpty()) {
            allTickets.toMutableList()
        } else {
            allTickets.filter { 
                it.eventName.contains(query, ignoreCase = true) || 
                it.ticketId.contains(query, ignoreCase = true) 
            }.toMutableList()
        }
        sortTickets() // Apply current sort to filtered results
    }

    private fun sortTickets() {
        val sortedList = if (isDateAscending) {
            filteredTickets.sortedBy { it.eventDate }
        } else {
            filteredTickets.sortedByDescending { it.eventDate }
        }
        adapter.submitList(sortedList)
        
        binding.btnSortTicket.rotation = if (isDateAscending) 0f else 180f
        updateEmptyState(binding.etSearchTicket.text.isNotEmpty())
    }

    private fun updateEmptyState(isSearching: Boolean) {
        if (adapter.itemCount == 0) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvTickets.visibility = View.GONE
            binding.tvEmptyText.text = if (isSearching) "Ticket not found" else "No tickets yet"
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvTickets.visibility = View.VISIBLE
        }
    }

    private fun loadDummyTickets() {
        allTickets.clear()
        allTickets.add(Ticket("CC-8924-XYZ", "1", "Seminar Nasional AI", "Seminar", "2026-06-20", "Aula FT Unram", "Confirmed"))
        allTickets.add(Ticket("CC-6543-ABC", "2", "Workshop Android", "Workshop", "2026-06-25", "Gedung Kuliah Bersama", "Confirmed"))
        allTickets.add(Ticket("CC-1122-DEF", "3", "Dies Natalis Unram", "Dies Natalis", "2026-05-15", "Lapangan Rektorat", "Confirmed"))
        
        filterTickets("")
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_ticket
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeMahasiswaActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                R.id.nav_ticket -> true
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
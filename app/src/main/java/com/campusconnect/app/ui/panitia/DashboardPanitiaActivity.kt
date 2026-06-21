package com.campusconnect.app.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.EventPanitiaAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.ui.panitia.DetailPanitiaEventActivity
import com.campusconnect.app.ui.profile.ProfileActivity

class DashboardPanitiaActivity : AppCompatActivity() {
    private val myEvents = mutableListOf<Event>()
    private lateinit var adapter: EventPanitiaAdapter
    private lateinit var rvPanitiaEvents: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_panitia)

        setupMyEventsList()
        loadPanitiaName()
        profilepanitia()
        createevent()
        showDefaultStats()
    }

    override fun onStart() {
        super.onStart()
        loadMyEventsStats()
        loadMyManagedEvents()
    }

    private fun setupMyEventsList() {
        rvPanitiaEvents = findViewById(R.id.rvPanitiaEvents)
        adapter = EventPanitiaAdapter(
            eventList = myEvents,
            onDetailClick = { event ->
                startActivity(Intent(this, DetailPanitiaEventActivity::class.java).apply {
                    putExtra("eventId", event.id)
                    putExtra("source", "panitia")
                })
            },
            onEditClick = { event ->
                startActivity(Intent(this, CreateEventActivity::class.java).apply {
                    putExtra("mode", "edit")
                    putExtra("eventId", event.id)
                })
            }
        )

        rvPanitiaEvents.layoutManager = LinearLayoutManager(this)
        rvPanitiaEvents.adapter = adapter
    }

    private fun loadPanitiaName() {
        val tvPanitiaName = findViewById<TextView>(R.id.tvPanitiaName)
        val user = SupabaseRepository.currentUser(this)

        if (user == null) {
            tvPanitiaName.text = "Hello, Pengguna!"
            return
        }

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            val fullName = result.getOrNull()?.fullName
                ?: user.fullName.ifBlank { user.email.substringBefore("@").ifBlank { "Pengguna" } }
            tvPanitiaName.text = "Hello, $fullName!"
        }
    }

    private fun profilepanitia() {
        val btnProfile = findViewById<ImageButton>(R.id.btnOpenProfile)
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun createevent() {
        val fabAddEvent = findViewById<ImageButton>(R.id.fabAddEvent)
        fabAddEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
    }

    private fun showDefaultStats() {
        findViewById<TextView>(R.id.tvTotalEvents).text = "0"
        findViewById<TextView>(R.id.tvTotalRegistrants).text = "0"
    }

    private fun loadMyEventsStats() {
        val user = SupabaseRepository.currentUser(this) ?: return
        SupabaseRepository.loadOrganizerStats(user.uid) { result ->
            result
                .onSuccess { stats ->
                    findViewById<TextView>(R.id.tvTotalEvents).text = stats.first.toString()
                    findViewById<TextView>(R.id.tvTotalRegistrants).text = stats.second.toString()
                }
                .onFailure {
                    showDefaultStats()
                }
        }
    }

    private fun loadMyManagedEvents() {
        val label = findViewById<TextView>(R.id.tvMyEventsLabel)
        val user = SupabaseRepository.currentUser(this) ?: run {
            myEvents.clear()
            adapter.notifyDataSetChanged()
            label.text = "My Managed Events"
            return
        }

        SupabaseRepository.loadOrganizerEvents(user.uid) { result ->
            result
                .onSuccess { events ->
                    myEvents.clear()
                    myEvents.addAll(events)
                    adapter.notifyDataSetChanged()
                    label.text = if (events.isEmpty()) {
                        "My Managed Events - No events yet"
                    } else {
                        "My Managed Events"
                    }
                }
                .onFailure { exception ->
                    myEvents.clear()
                    adapter.notifyDataSetChanged()
                    label.text = "My Managed Events - Failed to load"
                    Toast.makeText(this, exception.localizedMessage ?: "Gagal memuat event.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

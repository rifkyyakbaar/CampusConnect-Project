package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.model.Event
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeMahasiswaActivity : AppCompatActivity() {
    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: EventMahasiswaAdapter
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_mahasiswa)

        rvEvents = findViewById(R.id.rvEvents)

        adapter = EventMahasiswaAdapter(eventList) { event ->

            val intent = Intent(this, DetailEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)

        }

        rvEvents.layoutManager = LinearLayoutManager(this)
        rvEvents.adapter = adapter

        loadWelcomeName()
        loadEvents()
        setupBottomNavigation()
    }

    private fun loadWelcomeName() {
        val tvWelcomeName = findViewById<TextView>(R.id.tvWelcomeName)
        val user = SupabaseRepository.currentUser(this)

        if (user == null) {
            tvWelcomeName.text = getString(R.string.welcome_user, "Pengguna")
            return
        }

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            val fullName = result.getOrNull()?.fullName
                ?: user.fullName.ifBlank {
                    user.email.substringBefore("@").ifBlank { "Pengguna" }
                }

            tvWelcomeName.text =
                getString(R.string.welcome_user, fullName)
        }
    }

    private fun loadEvents() {

        SupabaseRepository.loadApprovedEvents { result ->

            result.onSuccess { events ->

                eventList.clear()
                eventList.addAll(events)
                adapter.notifyDataSetChanged()

            }

            result.onFailure {

                // optional Toast

            }
        }
    }

    private fun setupBottomNavigation() {

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> true

                R.id.nav_ticket -> {
                    startActivity(
                        Intent(this, TicketActivity::class.java)
                    )
                    true
                }

                R.id.nav_history -> {
                    startActivity(
                        Intent(this, HistoryActivity::class.java)
                    )
                    true
                }

                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                    true
                }

                else -> false
            }
        }
    }
}
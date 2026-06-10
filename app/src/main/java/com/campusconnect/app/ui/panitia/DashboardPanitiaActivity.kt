package com.campusconnect.app.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity

class DashboardPanitiaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_panitia)

        loadPanitiaName()
        profilepanitia()
        createevent()
        showDefaultStats()
    }

    override fun onStart() {
        super.onStart()
        loadMyEventsStats()
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
}

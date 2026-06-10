package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.profile.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeMahasiswaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_mahasiswa)

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
                ?: user.fullName.ifBlank { user.email.substringBefore("@").ifBlank { "Pengguna" } }
            tvWelcomeName.text = getString(R.string.welcome_user, fullName)
        }
    }

    private fun loadEvents() {
        SupabaseRepository.loadEvents {
            // TODO: hubungkan hasil event ke RecyclerView ketika UI daftar event sudah tersedia.
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_ticket -> {
                    startActivity(Intent(this, TicketActivity::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}

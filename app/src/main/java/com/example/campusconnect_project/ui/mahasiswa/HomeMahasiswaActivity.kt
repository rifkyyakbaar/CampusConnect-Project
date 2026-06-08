package com.example.campusconnect_project.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeMahasiswaActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_mahasiswa)

        loadWelcomeName()
        loadEvents()
        setupBottomNavigation()
    }

    private fun loadWelcomeName() {
        val tvWelcomeName = findViewById<TextView>(R.id.tvWelcomeName)
        val user = auth.currentUser

        if (user == null) {
            tvWelcomeName.text = getString(R.string.welcome_user, "Pengguna")
            return
        }

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName")
                    ?: user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "Pengguna"
                tvWelcomeName.text = getString(R.string.welcome_user, fullName)
            }
            .addOnFailureListener {
                val fallbackName = user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "Pengguna"
                tvWelcomeName.text = getString(R.string.welcome_user, fallbackName)
            }
    }
    private fun loadEvents() {

        firestore.collection("events")
            .get()
            .addOnSuccessListener {

                // tampilkan semua event
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

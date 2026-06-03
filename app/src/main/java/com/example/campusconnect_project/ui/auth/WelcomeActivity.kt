package com.example.campusconnect_project.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import android.content.Intent
import android.widget.Button
import com.example.campusconnect_project.ui.admin.DashboardAdminActivity
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity
import com.example.campusconnect_project.ui.panitia.DashboardPanitiaActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class WelcomeActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDashboardIfLoggedIn()
        setContentView(R.layout.activity_welcome)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }

    private fun openDashboardIfLoggedIn() {
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role") ?: "Mahasiswa"
                val target = when {
                    role.equals("Admin", ignoreCase = true) -> DashboardAdminActivity::class.java
                    role.equals("Panitia", ignoreCase = true) -> DashboardPanitiaActivity::class.java
                    else -> HomeMahasiswaActivity::class.java
                }

                startActivity(Intent(this, target).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
    }
}

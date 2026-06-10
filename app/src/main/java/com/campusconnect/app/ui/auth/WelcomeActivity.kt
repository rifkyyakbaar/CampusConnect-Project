package com.campusconnect.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.admin.DashboardAdminActivity
import com.campusconnect.app.ui.mahasiswa.HomeMahasiswaActivity
import com.campusconnect.app.ui.panitia.DashboardPanitiaActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDashboardIfLoggedIn()
        setContentView(R.layout.activity_welcome)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun openDashboardIfLoggedIn() {
        val user = SupabaseRepository.currentUser(this) ?: return
        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            val role = result.getOrNull()?.role ?: user.role
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

package com.example.campusconnect_project.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.campusconnect_project.R
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity


class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        btnGetStarted.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HomeMahasiswaActivity::class.java
                )
            )

        }

        tvGoToLogin.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }
}
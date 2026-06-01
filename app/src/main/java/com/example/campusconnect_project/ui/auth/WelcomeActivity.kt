package com.example.campusconnect_project.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import android.content.Intent
import android.widget.Button


class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
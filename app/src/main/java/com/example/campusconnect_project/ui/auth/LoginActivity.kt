package com.example.campusconnect_project.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        tvForgotPassword.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )
            )
        }

        btnSignIn.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    HomeMahasiswaActivity::class.java
                )
            )
        }

        tvSignUp.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }
}
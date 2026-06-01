package com.example.campusconnect_project.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import android.content.Intent
import android.widget.Button
import android.widget.ImageView


class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val btnBackReset = findViewById<ImageView>(R.id.btnBackReset)
        val btnConfirmReset = findViewById<Button>(R.id.btnConfirmReset)

        btnBackReset.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }

        btnConfirmReset.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }
}
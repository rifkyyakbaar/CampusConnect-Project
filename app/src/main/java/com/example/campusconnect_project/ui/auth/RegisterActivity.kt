package com.example.campusconnect_project.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import androidx.core.graphics.toColorInt

class RegisterActivity : AppCompatActivity() {
    private var selectedRole = "Mahasiswa"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnRoleMahasiswa = findViewById<Button>(R.id.btnRoleMahasiswa)
        val btnRolePanitia = findViewById<Button>(R.id.btnRolePanitia)
        val btnSignUpSubmit = findViewById<Button>(R.id.btnSignUpSubmit)
        val tvGoToSignIn = findViewById<TextView>(R.id.tvGoToSignIn)

        btnRoleMahasiswa.setOnClickListener {
            selectedRole = "Mahasiswa"
            btnRoleMahasiswa.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#0047A5".toColorInt()
                )
            btnRoleMahasiswa.setTextColor(
                android.graphics.Color.WHITE
            )
            btnRolePanitia.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.TRANSPARENT
                )
            btnRolePanitia.setTextColor(
                "#6B7280".toColorInt()
            )
        }

        btnRolePanitia.setOnClickListener {
            selectedRole = "Panitia"
            btnRolePanitia.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#0047A5".toColorInt()
                )
            btnRolePanitia.setTextColor(
                android.graphics.Color.WHITE
            )
            btnRoleMahasiswa.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.TRANSPARENT
                )
            btnRoleMahasiswa.setTextColor(
                "#6B7280".toColorInt()
            )
        }

        btnSignUpSubmit.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }

        tvGoToSignIn.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }
}
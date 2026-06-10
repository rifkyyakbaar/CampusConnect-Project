package com.campusconnect.app.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.campusconnect.app.data.SupabaseRepository


class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val btnBackForgot = findViewById<ImageView>(R.id.btnBackForgot)
        val btnSendReset = findViewById<Button>(R.id.btnSendReset)

        btnBackForgot.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
        btnSendReset.setOnClickListener {
            val email = findViewById<EditText>(R.id.etForgotEmail).text.toString().trim()
            if (email.isBlank()) {
                Toast.makeText(this, "Email wajib diisi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSendReset.isEnabled = false
            SupabaseRepository.sendPasswordReset(email) { result ->
                btnSendReset.isEnabled = true
                result
                    .onSuccess {
                        Toast.makeText(this, "Link reset password dikirim.", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    .onFailure { exception ->
                        Toast.makeText(this, exception.localizedMessage ?: "Gagal mengirim reset password.", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}

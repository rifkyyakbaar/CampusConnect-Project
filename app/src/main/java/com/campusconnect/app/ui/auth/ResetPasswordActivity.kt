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
            val password = findViewById<EditText>(R.id.etNewPassword).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.etConfirmNewPassword).text.toString()
            when {
                password.length < 6 -> Toast.makeText(this, "Password minimal 6 karakter.", Toast.LENGTH_SHORT).show()
                password != confirmPassword -> Toast.makeText(this, "Konfirmasi password tidak cocok.", Toast.LENGTH_SHORT).show()
                else -> {
                    btnConfirmReset.isEnabled = false
                    SupabaseRepository.updatePassword(this, password) { result ->
                        btnConfirmReset.isEnabled = true
                        result
                            .onSuccess {
                                Toast.makeText(this, "Password berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                            .onFailure { exception ->
                                Toast.makeText(this, exception.localizedMessage ?: "Gagal memperbarui password.", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }
}

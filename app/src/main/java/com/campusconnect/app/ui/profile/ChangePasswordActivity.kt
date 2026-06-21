package com.campusconnect.app.ui.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val btnBackPassword = findViewById<ImageView>(R.id.btnBackPassword)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnUpdatePassword = findViewById<Button>(R.id.btnUpdatePassword)

        btnBackPassword.setOnClickListener { finish() }

        btnUpdatePassword.setOnClickListener {
            val currentPassword = findViewById<EditText>(R.id.etCurrentPassword).text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // 1. Validasi Input Kosong
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validasi Panjang Password Baru
            if (newPassword.length < 6) {
                Toast.makeText(this, "Password baru minimal 6 karakter!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Validasi Kesamaan Password Baru
            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Konfirmasi password baru tidak cocok!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Matikan tombol sementara agar tidak double klik
            btnUpdatePassword.isEnabled = false
            btnUpdatePassword.text = "Memverifikasi..."

            // 4. Tahap Awal: Verifikasi Password Lama (Re-authenticate)
            val user = SupabaseRepository.currentUser(this)
            if (user == null) {
                Toast.makeText(this, "Sesi tidak valid, silakan login ulang.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            SupabaseRepository.signInWithEmail(this, user.email, currentPassword) { loginResult ->
                loginResult.onSuccess {
                    // Jika login sukses (password lama benar), baru lanjut update password
                    btnUpdatePassword.text = "Memperbarui..."

                    SupabaseRepository.updatePassword(this, newPassword) { updateResult ->
                        updateResult.onSuccess {
                            Toast.makeText(this, "Password berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            finish()
                        }.onFailure { exception ->
                            Toast.makeText(this, "Gagal update: ${exception.message}", Toast.LENGTH_LONG).show()
                            btnUpdatePassword.isEnabled = true
                            btnUpdatePassword.text = "Update Password"
                        }
                    }
                }.onFailure {
                    // Jika login gagal (password lama salah)
                    Toast.makeText(this, "Password saat ini salah!", Toast.LENGTH_SHORT).show()
                    btnUpdatePassword.isEnabled = true
                    btnUpdatePassword.text = "Update Password"
                }
            }
        }
    }
}
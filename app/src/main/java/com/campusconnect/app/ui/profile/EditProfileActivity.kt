package com.campusconnect.app.ui.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView // <-- Tambahan import untuk TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val btnBackEdit = findViewById<ImageView>(R.id.btnBackEdit)
        btnBackEdit.setOnClickListener { finish() }

        val etEditName = findViewById<EditText>(R.id.etEditName)
        val etEditEmail = findViewById<EditText>(R.id.etEditEmail)
        val etEditRole = findViewById<EditText>(R.id.etEditRole)
        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges)

        // <-- TAMBAHAN 1: Hubungkan TextView untuk judul nama di bawah avatar
        val tvEditNameTitle = findViewById<TextView>(R.id.tvEditNameTitle)

        // Tangkap data dari ProfileActivity
        val currentName = intent.getStringExtra("EXTRA_NAME")
        val currentEmail = intent.getStringExtra("EXTRA_EMAIL")
        val currentRole = intent.getStringExtra("EXTRA_ROLE")

        etEditName.setText(currentName)
        etEditEmail.setText(currentEmail)

        // <-- TAMBAHAN 2: Tampilkan nama asli di bawah foto profil
        tvEditNameTitle.text = currentName

        // Membersihkan format string untuk mengambil Role-nya saja
        val cleanRole = currentRole?.split(" - ")?.firstOrNull() ?: currentRole
        etEditRole.setText(cleanRole)

        // KUNCI KOLOM ROLE: Agar pengguna tidak bisa mengedit rolenya secara manual
        etEditRole.isEnabled = false
        etEditRole.isFocusable = false

        // LOGIKA TOMBOL SAVE CHANGES
        btnSaveChanges.setOnClickListener {
            val newName = etEditName.text.toString().trim()
            val newEmail = etEditEmail.text.toString().trim()

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Nama dan Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Matikan sementara untuk mencegah spam klik
            btnSaveChanges.isEnabled = false
            btnSaveChanges.text = "Menyimpan..."

            val user = SupabaseRepository.currentUser(this)
            if (user != null) {
                // MENGGUNAKAN FUNGSI BARU YANG BARU KITA TAMBAHKAN
                SupabaseRepository.updateUserNameAndEmail(this, user.uid, newName, newEmail) { result: Result<Unit> ->
                    result
                        .onSuccess {
                            Toast.makeText(this@EditProfileActivity, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            finish() // Kembali ke halaman Profile
                        }
                        .onFailure { exception ->
                            Toast.makeText(this@EditProfileActivity, "Gagal: ${exception.message}", Toast.LENGTH_LONG).show()
                            btnSaveChanges.isEnabled = true
                            btnSaveChanges.text = "Save Changes"
                        }
                }
            } else {
                Toast.makeText(this, "Sesi habis, silakan login ulang.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
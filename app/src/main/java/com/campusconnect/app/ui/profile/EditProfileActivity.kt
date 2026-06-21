package com.campusconnect.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class EditProfileActivity : AppCompatActivity() {

    // 1. Variabel untuk menyimpan gambar sementara dari galeri
    private var selectedImageUri: Uri? = null
    private lateinit var ivEditAvatar: ImageView

    // 2. Pemanggil Galeri HP
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri

            // --- KUNCI PENGHAPUS WARNA ABU-ABU XML ---
            ivEditAvatar.setPadding(0, 0, 0, 0)
            ivEditAvatar.imageTintList = null
            // -----------------------------------------

            Glide.with(this)
                .load(uri)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_camera)
                .into(ivEditAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val btnBackEdit = findViewById<ImageView>(R.id.btnBackEdit)
        btnBackEdit.setOnClickListener { finish() }

        val etEditName = findViewById<EditText>(R.id.etEditName)
        val etEditEmail = findViewById<EditText>(R.id.etEditEmail)
        val etEditRole = findViewById<EditText>(R.id.etEditRole)
        val btnSaveChanges = findViewById<Button>(R.id.btnSaveChanges)
        val tvEditNameTitle = findViewById<TextView>(R.id.tvEditNameTitle)

        // ID tambahan untuk upload foto
        val tvUploadPhoto = findViewById<TextView>(R.id.tvUploadPhoto)
        ivEditAvatar = findViewById(R.id.ivEditAvatar)

        // Tangkap data dari ProfileActivity
        val currentName = intent.getStringExtra("EXTRA_NAME")
        val currentEmail = intent.getStringExtra("EXTRA_EMAIL")
        val currentRole = intent.getStringExtra("EXTRA_ROLE")

        etEditName.setText(currentName)
        etEditEmail.setText(currentEmail)
        tvEditNameTitle.text = currentName

        // Membersihkan format string untuk mengambil Role-nya saja
        val cleanRole = currentRole?.split(" - ")?.firstOrNull() ?: currentRole
        etEditRole.setText(cleanRole)

        // Kunci kolom role
        etEditRole.isEnabled = false
        etEditRole.isFocusable = false

        // --- PERUBAHAN UTAMA ADA DI SINI (BAGIAN 3) ---
        // Kita tidak lagi mengambil dari SharedPreferences karena akan hilang saat logout.
        // Sebagai gantinya, kita ambil UID user saat ini, lalu menyusun URL-nya langsung.
        val user = SupabaseRepository.currentUser(this)
        if (user != null) {
            // Aplikasi langsung menebak URL foto dari server Supabase menggunakan UID
            val avatarUrl = SupabaseRepository.getAvatarUrl(user.uid)

            // Panggil loadImageFromUrl. Fungsi ObjectKey (Anti-Cache)
            // sudah ada di dalam fungsi loadImageFromUrl di bawah.
            loadImageFromUrl(avatarUrl, ivEditAvatar)
        }
        // -----------------------------------------------

        // 4. Klik tulisan biru untuk membuka galeri
        tvUploadPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 5. LOGIKA TOMBOL SAVE CHANGES
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

            val currentUser = SupabaseRepository.currentUser(this)
            if (currentUser != null) {
                // --- SKENARIO A: Jika User Memilih Foto Baru ---
                if (selectedImageUri != null) {
                    SupabaseRepository.uploadUserAvatar(this, selectedImageUri!!, currentUser.uid) { uploadResult: Result<String> ->
                        uploadResult.onSuccess {
                            // Jika upload foto berhasil, baru simpan data nama & email
                            saveProfileData(currentUser.uid, newName, newEmail, btnSaveChanges)
                        }.onFailure { exception ->
                            Toast.makeText(this, "Gagal upload foto: ${exception.message}", Toast.LENGTH_LONG).show()
                            btnSaveChanges.isEnabled = true
                            btnSaveChanges.text = "Save Changes"
                        }
                    }
                }
                // --- SKENARIO B: Jika User Hanya Edit Nama (Tanpa ganti foto) ---
                else {
                    saveProfileData(currentUser.uid, newName, newEmail, btnSaveChanges)
                }
            } else {
                Toast.makeText(this, "Sesi habis, silakan login ulang.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Fungsi Pembantu (Helper) untuk mengeksekusi update nama & email
    private fun saveProfileData(uid: String, name: String, email: String, btnSaveChanges: Button) {
        SupabaseRepository.updateUserNameAndEmail(this, uid, name, email) { result ->
            result.onSuccess {
                Toast.makeText(this@EditProfileActivity, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish() // Kembali ke halaman Profile
            }.onFailure { exception ->
                Toast.makeText(this@EditProfileActivity, "Gagal: ${exception.message}", Toast.LENGTH_LONG).show()
                btnSaveChanges.isEnabled = true
                btnSaveChanges.text = "Save Changes"
            }
        }
    }

    // Fungsi Pemuat Gambar Internet SAKTI (Menggunakan Glide)
    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        // --- KUNCI PENGHAPUS WARNA ABU-ABU XML ---
        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        // -----------------------------------------

        // Trik agar Glide selalu memuat foto terbaru menggunakan timestamp, bukan cache lama
        val signature = ObjectKey(System.currentTimeMillis().toString())

        Glide.with(imageView.context)
            .load(url)
            .signature(signature) // Memaksa refresh gambar terbaru
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_camera)
            .error(android.R.drawable.ic_menu_camera) // Jika gagal/belum ada foto, kembali ke ikon kamera
            .into(imageView)
    }
}
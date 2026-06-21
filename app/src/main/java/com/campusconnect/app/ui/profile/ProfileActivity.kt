package com.campusconnect.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.auth.LoginActivity
import com.campusconnect.app.ui.mahasiswa.HomeMahasiswaActivity
import com.campusconnect.app.ui.mahasiswa.HistoryActivity
import com.campusconnect.app.ui.mahasiswa.ManageTicketActivity
import com.campusconnect.app.ui.panitia.DashboardPanitiaActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileActivity : AppCompatActivity() {
    private var userRole = ""
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var tvDeleteAccount: TextView
    private lateinit var tvEditProfile: TextView

    // Variabel penampung foto profil
    private lateinit var ivUserAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

//        val mainView = findViewById<android.view.View>(R.id.main)
//        if (mainView != null) {
//            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
//                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//                insets
//            }
//        }

        // Inisialisasi View
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileRole = findViewById(R.id.tvProfileRole)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvEditProfile = findViewById(R.id.tvEditProfile)
        btnLogout = findViewById(R.id.btnLogout)
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount)

        // Hubungkan ID foto profil
        ivUserAvatar = findViewById(R.id.ivUserAvatar)

        loadProfile()

        btnLogout.setOnClickListener {
            logout()
        }

        tvDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }

        tvEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("EXTRA_NAME", tvProfileName.text.toString())
            intent.putExtra("EXTRA_EMAIL", tvProfileEmail.text.toString())
            intent.putExtra("EXTRA_ROLE", tvProfileRole.text.toString())
            startActivity(intent)
        }

        setupBottomNavigation()
    }

    // onResume agar halaman otomatis me-refresh foto/nama setelah dari halaman Edit Profil
    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val user = SupabaseRepository.currentUser(this) ?: run {
            openLogin()
            return
        }

        tvProfileName.text = user.fullName.ifBlank { "Pengguna" }
        tvProfileEmail.text = user.email.ifBlank { "-" }

        // --- PERUBAHAN UTAMA ADA DI SINI ---
        // Aplikasi langsung menebak URL gambar dari server Supabase menggunakan UID
        // Jadi, foto tidak akan pernah hilang meskipun cache atau SharedPreferences dibersihkan (Logout)
        val avatarUrl = SupabaseRepository.getAvatarUrl(user.uid)
        loadImageFromUrl(avatarUrl, ivUserAvatar)
        // -----------------------------------

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            result
                .onSuccess { profile ->
                    userRole = profile.role
                    tvProfileName.text = profile.fullName.ifBlank { "Pengguna" }
                    tvProfileRole.text = "${profile.role} - ${providerLabel(profile.provider)}"
                    tvProfileEmail.text = profile.email.ifBlank { "-" }
                }
                .onFailure { exception ->
                    val errorMsg = exception.localizedMessage ?: ""
                    if (errorMsg.contains("JWT", ignoreCase = true) || errorMsg.contains("expired", ignoreCase = true)) {
                        Toast.makeText(this, "Sesi telah habis, silakan login kembali", Toast.LENGTH_LONG).show()
                        logout()
                    } else {
                        tvProfileRole.text = providerLabel(user.provider)
                        showMessage(errorMsg.ifBlank { "Gagal memuat profil." })
                    }
                }
        }
    }

    private fun logout() {
        SupabaseRepository.signOut(this)
        val client = GoogleSignIn.getClient(this, googleSignInOptions())
        client.signOut().addOnCompleteListener {
            openLogin()
        }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Hapus akun?")
            .setMessage("Akun akan dinonaktifkan dan tidak bisa digunakan untuk login lagi.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                deleteAccount()
            }
            .show()
    }

    private fun deleteAccount() {
        setAccountActionEnabled(false)
        SupabaseRepository.deleteCurrentUser(this) { result ->
            result
                .onSuccess {
                    showMessage("Akun berhasil dihapus.")
                    GoogleSignIn.getClient(this, googleSignInOptions()).signOut()
                    openLogin()
                }
                .onFailure { exception ->
                    showMessage(exception.localizedMessage ?: "Gagal menghapus akun.")
                    setAccountActionEnabled(true)
                }
        }
    }

    private fun googleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private fun providerLabel(provider: String): String {
        return if (provider.equals("google", ignoreCase = true)) "Google" else "Email/Password"
    }

    private fun setAccountActionEnabled(isEnabled: Boolean) {
        btnLogout.isEnabled = isEnabled
        tvDeleteAccount.isEnabled = isEnabled
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_profile

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true

                R.id.nav_home -> {
                    if (userRole.equals("Panitia", ignoreCase = true)) {
                        startActivity(Intent(this, DashboardPanitiaActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        startActivity(Intent(this, HomeMahasiswaActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    }
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_ticket -> {
                    startActivity(Intent(this, ManageTicketActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Fungsi Pemuat Gambar Internet SAKTI (Menggunakan Glide)
    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        // --- DUA BARIS KUNCI PENGHAPUS ABU-ABU ---
        imageView.setPadding(0, 0, 0, 0)
        imageView.imageTintList = null
        // -----------------------------------------

        // Trik agar Glide selalu memuat foto terbaru, bukan cache lama
        val signature = ObjectKey(System.currentTimeMillis().toString())

        Glide.with(imageView.context)
            .load(url)
            .signature(signature)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_camera)
            .error(android.R.drawable.ic_menu_camera) // Jika gagal/belum ada foto, kembali ke ikon kamera
            .into(imageView)
    }
}
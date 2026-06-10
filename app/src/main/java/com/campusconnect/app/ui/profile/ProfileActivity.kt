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
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.auth.LoginActivity
import com.campusconnect.app.ui.mahasiswa.HomeMahasiswaActivity
import com.campusconnect.app.ui.panitia.DashboardPanitiaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileActivity : AppCompatActivity() {
    private var userRole = ""
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var tvDeleteAccount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileRole = findViewById(R.id.tvProfileRole)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        btnLogout = findViewById(R.id.btnLogout)
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            if (userRole.equals("Panitia", ignoreCase = true)) {
                startActivity(Intent(this, DashboardPanitiaActivity::class.java))
            } else {
                startActivity(Intent(this, HomeMahasiswaActivity::class.java))
            }
            finish()
        }

        loadProfile()
        btnLogout.setOnClickListener {
            logout()
        }

        tvDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }
    }

    private fun loadProfile() {
        val user = SupabaseRepository.currentUser(this) ?: run {
            openLogin()
            return
        }

        tvProfileName.text = user.fullName.ifBlank { "Pengguna" }
        tvProfileEmail.text = user.email.ifBlank { "-" }

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            result
                .onSuccess { profile ->
                    userRole = profile.role
                    tvProfileName.text = profile.fullName.ifBlank { "Pengguna" }
                    tvProfileRole.text = "${profile.role} - ${providerLabel(profile.provider)}"
                    tvProfileEmail.text = profile.email.ifBlank { "-" }
                }
                .onFailure { exception ->
                    tvProfileRole.text = providerLabel(user.provider)
                    showMessage(exception.localizedMessage ?: "Gagal memuat profil.")
                }
        }
    }

    private fun logout() {
        SupabaseRepository.signOut(this)
        GoogleSignIn.getClient(this, googleSignInOptions()).signOut()
            .addOnCompleteListener {
                openLogin()
            }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Hapus akun?")
            .setMessage("Data profil Supabase akan dihapus permanen. Penghapusan user Auth membutuhkan aturan Supabase yang mengizinkan endpoint delete user.")
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

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

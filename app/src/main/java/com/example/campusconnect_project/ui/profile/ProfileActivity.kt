package com.example.campusconnect_project.ui.profile

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.auth.LoginActivity
import com.example.campusconnect_project.ui.panitia.DashboardPanitiaActivity
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var userRole = ""
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRole: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var btnLogout: Button
    private lateinit var tvDeleteAccount: TextView

    private val googleReauthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            showMessage("Autentikasi ulang Google gagal.")
            setAccountActionEnabled(true)
            return@registerForActivityResult
        }

        val user = auth.currentUser ?: run {
            showMessage("User tidak ditemukan.")
            setAccountActionEnabled(true)
            return@registerForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(task.result.idToken, null)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                deleteAccount(user.uid)
            }
            .addOnFailureListener { exception ->
                showMessage(exception.localizedMessage ?: "Autentikasi ulang gagal.")
                setAccountActionEnabled(true)
            }
    }

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

                startActivity(
                    Intent(
                        this,
                        DashboardPanitiaActivity::class.java
                    )
                )

            } else {

                startActivity(
                    Intent(
                        this,
                        HomeMahasiswaActivity::class.java
                    )
                )
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
        val user = auth.currentUser ?: run {
            openLogin()
            return
        }

        tvProfileName.text = user.displayName ?: "Pengguna"
        tvProfileEmail.text = user.email ?: "-"

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName")
                    ?: user.displayName
                    ?: user.email?.substringBefore("@")
                    ?: "Pengguna"
                val role = document.getString("role") ?: "Mahasiswa"
                userRole = role
                val provider = document.getString("provider") ?: providerLabel()

                tvProfileName.text = fullName
                tvProfileRole.text = "$role - $provider"
                tvProfileEmail.text = user.email ?: document.getString("email") ?: "-"
            }
            .addOnFailureListener { exception ->
                tvProfileRole.text = providerLabel()
                showMessage(exception.localizedMessage ?: "Gagal memuat profil.")
            }
    }
    private fun logout() {
        auth.signOut()
        GoogleSignIn.getClient(this, googleSignInOptions()).signOut()
            .addOnCompleteListener {
                openLogin()
            }
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("Hapus akun?")
            .setMessage("Akun Firebase Auth dan data profil di Firestore akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                startDeleteAccount()
            }
            .show()
    }

    private fun startDeleteAccount() {
        val user = auth.currentUser ?: run {
            openLogin()
            return
        }

        setAccountActionEnabled(false)
        if (isGoogleUser()) {
            googleReauthLauncher.launch(googleSignInIntent())
            return
        }

        val email = user.email
        if (email.isNullOrBlank()) {
            showMessage("Email user tidak ditemukan.")
            setAccountActionEnabled(true)
            return
        }

        showPasswordDialog(email)
    }

    private fun showPasswordDialog(email: String) {
        val passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi password")
            .setMessage("Masukkan password untuk menghapus akun.")
            .setView(passwordInput)
            .setNegativeButton("Batal") { _, _ ->
                setAccountActionEnabled(true)
            }
            .setPositiveButton("Hapus") { _, _ ->
                reauthenticateEmailUser(email, passwordInput.text.toString())
            }
            .show()
    }

    private fun reauthenticateEmailUser(email: String, password: String) {
        if (password.isBlank()) {
            showMessage("Password wajib diisi.")
            setAccountActionEnabled(true)
            return
        }

        val user = auth.currentUser ?: run {
            openLogin()
            return
        }
        val credential = EmailAuthProvider.getCredential(email, password)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                deleteAccount(user.uid)
            }
            .addOnFailureListener { exception ->
                showMessage(exception.localizedMessage ?: "Password tidak valid.")
                setAccountActionEnabled(true)
            }
    }

    private fun deleteAccount(uid: String) {
        val user = auth.currentUser ?: run {
            openLogin()
            return
        }

        firestore.collection("users").document(uid)
            .delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        showMessage("Akun berhasil dihapus.")
                        GoogleSignIn.getClient(this, googleSignInOptions()).signOut()
                        openLogin()
                    }
                    .addOnFailureListener { exception ->
                        showMessage(exception.localizedMessage ?: "Gagal menghapus akun auth.")
                        setAccountActionEnabled(true)
                    }
            }
            .addOnFailureListener { exception ->
                showMessage(exception.localizedMessage ?: "Gagal menghapus data profil.")
                setAccountActionEnabled(true)
            }
    }

    private fun googleSignInIntent(): Intent {
        return GoogleSignIn.getClient(this, googleSignInOptions()).signInIntent
    }

    private fun googleSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    private fun isGoogleUser(): Boolean {
        return auth.currentUser?.providerData?.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        } ?: false
    }

    private fun providerLabel(): String {
        return if (isGoogleUser()) "Google" else "Email/Password"
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

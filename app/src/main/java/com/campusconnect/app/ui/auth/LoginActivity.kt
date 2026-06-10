package com.campusconnect.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.ui.admin.DashboardAdminActivity
import com.campusconnect.app.ui.mahasiswa.HomeMahasiswaActivity
import com.campusconnect.app.ui.panitia.DashboardPanitiaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleSignIn: Button

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            val exception = task.exception
            exception?.printStackTrace()

            val statusCode = (exception as? ApiException)?.statusCode
            showError("Google sign in gagal. Kode: $statusCode")
            return@registerForActivityResult
        }

        val account = task.result
        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            showError("Google ID token tidak ditemukan.")
            setLoading(false)
            return@registerForActivityResult
        }

        SupabaseRepository.signInWithGoogle(
            context = this,
            idToken = idToken,
            fullName = account.displayName ?: "Pengguna",
            email = account.email ?: "",
            role = "Mahasiswa"
        ) { result ->
            result
                .onSuccess { user -> openDashboard(user.role) }
                .onFailure { exception ->
                    showError(exception.localizedMessage ?: "Google sign in gagal.")
                    setLoading(false)
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnSignIn.setOnClickListener {
            loginWithEmail()
        }

        btnGoogleSignIn.setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInIntent())
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithEmail() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString()

        if (email == "admin@campusconnect.com" && password == "admin123") {
            startActivity(Intent(this, DashboardAdminActivity::class.java))
            finish()
            return
        }

        when {
            email.isEmpty() -> showError("Email wajib diisi.")
            password.isEmpty() -> showError("Password wajib diisi.")
            else -> {
                setLoading(true)
                SupabaseRepository.signInWithEmail(this, email, password) { result ->
                    result
                        .onSuccess { user -> openDashboard(user.role) }
                        .onFailure { exception ->
                            showError(exception.localizedMessage ?: "Login gagal.")
                            setLoading(false)
                        }
                }
            }
        }
    }

    private fun googleSignInIntent(): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(this, options).signInIntent
    }

    private fun openDashboard(role: String) {
        val target = when {
            role.equals("Admin", ignoreCase = true) -> DashboardAdminActivity::class.java
            role.equals("Panitia", ignoreCase = true) -> DashboardPanitiaActivity::class.java
            else -> HomeMahasiswaActivity::class.java
        }

        startActivity(Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignIn.isEnabled = !isLoading
        btnGoogleSignIn.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

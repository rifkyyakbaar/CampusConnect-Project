package com.example.campusconnect_project.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.admin.DashboardAdminActivity
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity
import com.example.campusconnect_project.ui.panitia.DashboardPanitiaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class LoginActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var btnSignIn: Button
    private lateinit var btnGoogleSignIn: Button

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            showError("Google sign in gagal.")
            setLoading(false)
            return@registerForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(task.result.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                ensureUserProfileThenOpenDashboard()
            }
            .addOnFailureListener { exception ->
                showError(exception.localizedMessage ?: "Google sign in gagal.")
                setLoading(false)
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
            startActivity(
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )
            )
        }

        btnSignIn.setOnClickListener {
            loginWithEmail()
        }

        btnGoogleSignIn.setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInIntent())
        }

        tvSignUp.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun loginWithEmail() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPassword).text.toString()

        when {
            email.isEmpty() -> showError("Email wajib diisi.")
            password.isEmpty() -> showError("Password wajib diisi.")
            else -> {
                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        ensureUserProfileThenOpenDashboard()
                    }
                    .addOnFailureListener { exception ->
                        showError(exception.localizedMessage ?: "Login gagal.")
                        setLoading(false)
                    }
            }
        }
    }

    private fun ensureUserProfileThenOpenDashboard() {
        val user = auth.currentUser ?: run {
            showError("User tidak ditemukan.")
            setLoading(false)
            return
        }

        val userRef = firestore.collection("users").document(user.uid)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: "Mahasiswa"
                    openDashboard(role)
                    return@addOnSuccessListener
                }

                val defaultRole = "Mahasiswa"
                val userData = hashMapOf(
                    "uid" to user.uid,
                    "fullName" to (user.displayName ?: "Pengguna"),
                    "email" to (user.email ?: ""),
                    "role" to defaultRole,
                    "provider" to "google",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                userRef.set(userData)
                    .addOnSuccessListener {
                        openDashboard(defaultRole)
                    }
                    .addOnFailureListener { exception ->
                        showError(exception.localizedMessage ?: "Gagal menyimpan profil pengguna.")
                        setLoading(false)
                    }
            }
            .addOnFailureListener { exception ->
                showError(exception.localizedMessage ?: "Gagal mengambil profil pengguna.")
                setLoading(false)
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

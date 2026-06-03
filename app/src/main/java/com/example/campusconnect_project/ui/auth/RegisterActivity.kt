package com.example.campusconnect_project.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.mahasiswa.HomeMahasiswaActivity
import com.example.campusconnect_project.ui.panitia.DashboardPanitiaActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private var selectedRole = "Mahasiswa"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var btnSignUpSubmit: Button
    private lateinit var btnGoogleSignUp: Button

    private val googleSignUpLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            showError("Google sign up gagal.")
            setLoading(false)
            return@registerForActivityResult
        }

        val credential = GoogleAuthProvider.getCredential(task.result.idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                val user = auth.currentUser ?: return@addOnSuccessListener
                saveUserProfile(
                    uid = user.uid,
                    fullName = user.displayName ?: "Pengguna",
                    email = user.email ?: "",
                    role = selectedRole,
                    provider = "google"
                )
            }
            .addOnFailureListener { exception ->
                showError(exception.localizedMessage ?: "Google sign up gagal.")
                setLoading(false)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnRoleMahasiswa = findViewById<Button>(R.id.btnRoleMahasiswa)
        val btnRolePanitia = findViewById<Button>(R.id.btnRolePanitia)
        btnSignUpSubmit = findViewById(R.id.btnSignUpSubmit)
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp)
        val tvGoToSignIn = findViewById<TextView>(R.id.tvGoToSignIn)

        btnRoleMahasiswa.setOnClickListener {
            selectedRole = "Mahasiswa"
            btnRoleMahasiswa.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#0047A5".toColorInt()
                )
            btnRoleMahasiswa.setTextColor(
                android.graphics.Color.WHITE
            )
            btnRolePanitia.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.TRANSPARENT
                )
            btnRolePanitia.setTextColor(
                "#6B7280".toColorInt()
            )
        }

        btnRolePanitia.setOnClickListener {
            selectedRole = "Panitia"
            btnRolePanitia.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#0047A5".toColorInt()
                )
            btnRolePanitia.setTextColor(
                android.graphics.Color.WHITE
            )
            btnRoleMahasiswa.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.TRANSPARENT
                )
            btnRoleMahasiswa.setTextColor(
                "#6B7280".toColorInt()
            )
        }

        btnSignUpSubmit.setOnClickListener {
            registerWithEmail()
        }

        btnGoogleSignUp.setOnClickListener {
            setLoading(true)
            googleSignUpLauncher.launch(googleSignInIntent())
        }

        tvGoToSignIn.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }

    private fun registerWithEmail() {
        val fullName = findViewById<EditText>(R.id.etFullName).text.toString().trim()
        val email = findViewById<EditText>(R.id.etEmailRegister).text.toString().trim()
        val password = findViewById<EditText>(R.id.etPasswordRegister).text.toString()
        val confirmPassword = findViewById<EditText>(R.id.etConfirmPassword).text.toString()
        val termsAccepted = findViewById<CheckBox>(R.id.cbTerms).isChecked

        when {
            fullName.isEmpty() -> showError("Nama lengkap wajib diisi.")
            email.isEmpty() -> showError("Email wajib diisi.")
            password.length < 6 -> showError("Password minimal 6 karakter.")
            password != confirmPassword -> showError("Konfirmasi password tidak cocok.")
            !termsAccepted -> showError("Setujui syarat dan ketentuan terlebih dahulu.")
            else -> {
                setLoading(true)
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val user = auth.currentUser ?: return@addOnSuccessListener
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user.updateProfile(profileUpdates)
                        saveUserProfile(user.uid, fullName, email, selectedRole, "email")
                    }
                    .addOnFailureListener { exception ->
                        showError(exception.localizedMessage ?: "Registrasi gagal.")
                        setLoading(false)
                    }
            }
        }
    }

    private fun saveUserProfile(
        uid: String,
        fullName: String,
        email: String,
        role: String,
        provider: String
    ) {
        val userData = hashMapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "role" to role,
            "provider" to provider,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(uid)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Registrasi berhasil.", Toast.LENGTH_SHORT).show()
                openDashboard(role)
            }
            .addOnFailureListener { exception ->
                showError(exception.localizedMessage ?: "Gagal menyimpan profil pengguna.")
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
        val target = if (role.equals("Panitia", ignoreCase = true)) {
            DashboardPanitiaActivity::class.java
        } else {
            HomeMahasiswaActivity::class.java
        }

        startActivity(Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignUpSubmit.isEnabled = !isLoading
        btnGoogleSignUp.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

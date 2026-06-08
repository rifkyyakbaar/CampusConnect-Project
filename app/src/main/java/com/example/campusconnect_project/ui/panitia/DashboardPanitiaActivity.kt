    package com.example.campusconnect_project.ui.panitia

    import android.content.Intent
    import android.os.Bundle
    import android.widget.ImageButton
    import androidx.appcompat.app.AppCompatActivity
    import com.example.campusconnect_project.R
    import com.example.campusconnect_project.ui.profile.ProfileActivity
    import android.widget.TextView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore

    class DashboardPanitiaActivity : AppCompatActivity() {
        private val auth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_dashboard_panitia)

            loadPanitiaName()
            profilepanitia()
            createevent()
            loadMyEvents()
        }

        private fun loadPanitiaName() {
            val tvPanitiaName = findViewById<TextView>(R.id.tvPanitiaName)
            val user = auth.currentUser

            if (user == null) {
                tvPanitiaName.text = "Hello, Pengguna!"
                return
            }

            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val fullName = document.getString("fullName")
                        ?: user.displayName
                        ?: user.email?.substringBefore("@")
                        ?: "Pengguna"

                    tvPanitiaName.text = "Hello, $fullName!"
                }
                .addOnFailureListener {
                    tvPanitiaName.text = "fHello, Pengguna!"
                }
        }

        private fun profilepanitia() {
            val btnProfile = findViewById<ImageButton>(R.id.btnOpenProfile)

            btnProfile.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        ProfileActivity::class.java
                    )
                )
            }
        }

        private fun createevent() {
            val fabAddEvent = findViewById<ImageButton>(R.id.fabAddEvent)

            fabAddEvent.setOnClickListener {
                startActivity(
                    Intent(
                        this,
                        CreateEventActivity::class.java
                    )
                )
            }
        }
        private fun loadMyEvents() {

            val user = auth.currentUser ?: return

            firestore.collection("events")
                .whereEqualTo(
                    "organizerId",
                    user.uid
                )
                .get()
                .addOnSuccessListener { result ->

                    val totalEvents = result.size()

                    findViewById<TextView>(
                        R.id.tvTotalEvents
                    ).text = totalEvents.toString()
                }
        }
    }
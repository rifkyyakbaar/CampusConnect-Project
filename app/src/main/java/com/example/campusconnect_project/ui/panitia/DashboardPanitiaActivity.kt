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
    import com.google.firebase.firestore.ListenerRegistration

    class DashboardPanitiaActivity : AppCompatActivity() {
        private val auth = FirebaseAuth.getInstance()
        private val firestore = FirebaseFirestore.getInstance()
        private var myEventsListener: ListenerRegistration? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_dashboard_panitia)

            loadPanitiaName()
            profilepanitia()
            createevent()
            showDefaultStats()
        }

        override fun onStart() {
            super.onStart()
            listenMyEvents()
        }

        override fun onStop() {
            super.onStop()
            myEventsListener?.remove()
            myEventsListener = null
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
        private fun showDefaultStats() {
            findViewById<TextView>(R.id.tvTotalEvents).text = "0"
            findViewById<TextView>(R.id.tvTotalRegistrants).text = "0"
        }

        private fun listenMyEvents() {

            val user = auth.currentUser ?: return

            myEventsListener?.remove()
            myEventsListener = firestore.collection("events")
                .whereEqualTo(
                    "organizerId",
                    user.uid
                )
                .addSnapshotListener { result, error ->
                    if (error != null || result == null) {
                        showDefaultStats()
                        return@addSnapshotListener
                    }

                    val totalEvents = result.size()
                    val totalRegistrants = result.documents.sumOf { document ->
                        document.getLong("registrants") ?: 0L
                    }

                    findViewById<TextView>(
                        R.id.tvTotalEvents
                    ).text = totalEvents.toString()

                    findViewById<TextView>(
                        R.id.tvTotalRegistrants
                    ).text = totalRegistrants.toString()
                }
        }
    }

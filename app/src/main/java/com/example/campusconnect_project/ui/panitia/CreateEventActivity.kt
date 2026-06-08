package com.example.campusconnect_project.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class CreateEventActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        backdashboard()

        val btnSubmit =
            findViewById<Button>(R.id.btnSubmitEvent)

        btnSubmit.setOnClickListener {
            publishEvent()
        }
    }

    private fun backdashboard() {
        val btnBackCreate = findViewById<ImageView>(R.id.btnBackCreate)

        btnBackCreate.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    DashboardPanitiaActivity::class.java
                )
            )
        }
    }

    private fun publishEvent() {

        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)

        val eventName =
            findViewById<EditText>(R.id.etEventName).text.toString().trim()

        val category =
            findViewById<EditText>(R.id.etCategory).text.toString().trim()

        val capacityText =
            findViewById<EditText>(R.id.etCapacity).text.toString().trim()

        val description =
            findViewById<EditText>(R.id.etDescription).text.toString().trim()

        if (
            eventName.isEmpty() ||
            category.isEmpty() ||
            capacityText.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Lengkapi semua data",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val capacity = capacityText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(
                this,
                "Kapasitas harus berupa angka lebih dari 0",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(
                this,
                "Silakan login terlebih dahulu",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnSubmit.isEnabled = false

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->

                val organizerName =
                    doc.getString("fullName") ?: "Panitia"

                val eventRef = firestore.collection("events").document()
                val event = hashMapOf(
                    "eventId" to eventRef.id,
                    "eventName" to eventName,
                    "category" to category,
                    "capacity" to capacity,
                    "description" to description,
                    "organizerId" to user.uid,
                    "organizerName" to organizerName,
                    "posterUrl" to "",
                    "status" to "pending",
                    "registrants" to 0,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                eventRef
                    .set(event)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Event berhasil dibuat",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(
                            this,
                            "Gagal: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        btnSubmit.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal mengambil data panitia: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnSubmit.isEnabled = true
            }
    }
}

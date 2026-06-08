package com.example.campusconnect_project.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText
import android.widget.Toast
import android.widget.Button



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

        val eventName =
            findViewById<EditText>(R.id.etEventName).text.toString()

        val category =
            findViewById<EditText>(R.id.etCategory).text.toString()

        val capacity =
            findViewById<EditText>(R.id.etCapacity).text.toString()

        val description =
            findViewById<EditText>(R.id.etDescription).text.toString()

        if (
            eventName.isEmpty() ||
            category.isEmpty() ||
            capacity.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Lengkapi semua data",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val user = auth.currentUser ?: return

        firestore.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->

                val organizerName =
                    doc.getString("fullName") ?: "Panitia"

                val event = hashMapOf(
                    "eventName" to eventName,
                    "category" to category,
                    "capacity" to capacity.toInt(),
                    "description" to description,
                    "organizerId" to user.uid,
                    "organizerName" to organizerName,
                    "status" to "pending",
                    "registrants" to 0,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("events")
                    .add(event)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Event berhasil dibuat",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
            }
    }
}
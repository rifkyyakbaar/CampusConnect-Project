package com.campusconnect.app.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class CreateEventActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        backdashboard()

        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        btnSubmit.setOnClickListener {
            publishEvent()
        }
    }

    private fun backdashboard() {
        val btnBackCreate = findViewById<ImageView>(R.id.btnBackCreate)
        btnBackCreate.setOnClickListener {
            startActivity(Intent(this, DashboardPanitiaActivity::class.java))
        }
    }

    private fun publishEvent() {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        val eventName = findViewById<EditText>(R.id.etEventName).text.toString().trim()
        val category = findViewById<EditText>(R.id.etCategory).text.toString().trim()
        val capacityText = findViewById<EditText>(R.id.etCapacity).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()

        if (eventName.isEmpty() || category.isEmpty() || capacityText.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Kapasitas harus berupa angka lebih dari 0", Toast.LENGTH_SHORT).show()
            return
        }

        if (SupabaseRepository.currentUser(this) == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        SupabaseRepository.createEvent(this, eventName, category, capacity, description) { result ->
            result
                .onSuccess {
                    Toast.makeText(this, "Event berhasil dibuat", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { exception ->
                    Toast.makeText(this, "Gagal: ${exception.message}", Toast.LENGTH_LONG).show()
                    btnSubmit.isEnabled = true
                }
        }
    }
}

package com.campusconnect.app.ui.panitia

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class CreateEventActivity : AppCompatActivity() {
    private var selectedPosterUri: Uri? = null

    private val pickPosterLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        selectedPosterUri = uri
        findViewById<ImageView>(R.id.ivPosterPreview).apply {
            setImageURI(uri)
            visibility = ImageView.VISIBLE
        }
        findViewById<LinearLayout>(R.id.layoutUploadPosterPlaceholder).visibility = LinearLayout.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        backdashboard()
        setupPosterPicker()
        ensurePanitiaAccess()

        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        btnSubmit.setOnClickListener {
            publishEvent()
        }
    }

    private fun backdashboard() {
        val btnBackCreate = findViewById<ImageView>(R.id.btnBackCreate)
        btnBackCreate.setOnClickListener {
            finish()
        }
    }

    private fun setupPosterPicker() {
        findViewById<CardView>(R.id.cvUploadPoster).setOnClickListener {
            pickPosterLauncher.launch("image/*")
        }
    }

    private fun publishEvent() {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        val eventName = findViewById<EditText>(R.id.etEventName).text.toString().trim()
        val category = findViewById<EditText>(R.id.etCategory).text.toString().trim()
        val location = findViewById<EditText>(R.id.etLocation).text.toString().trim()
        val capacityText = findViewById<EditText>(R.id.etCapacity).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()

        if (eventName.isEmpty() || category.isEmpty() || location.isEmpty() || capacityText.isEmpty() || description.isEmpty()) {
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

        val user = SupabaseRepository.currentUser(this)
        if (user?.role?.equals("Panitia", ignoreCase = true) != true) {
            Toast.makeText(this, "Hanya akun Panitia yang bisa membuat event", Toast.LENGTH_SHORT).show()
            return
        }

        setSubmitLoading(btnSubmit, true)
        SupabaseRepository.createEvent(this, eventName, category, location, capacity, description, selectedPosterUri) { result ->
            result
                .onSuccess {
                    Toast.makeText(this, "Event berhasil dibuat", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { exception ->
                    showPublishError(exception.message.orEmpty())
                    setSubmitLoading(btnSubmit, false)
                }
        }
    }

    private fun ensurePanitiaAccess() {
        val user = SupabaseRepository.currentUser(this) ?: return
        if (!user.role.equals("Panitia", ignoreCase = true)) {
            Toast.makeText(this, "Hanya akun Panitia yang bisa membuat event", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setSubmitLoading(button: Button, loading: Boolean) {
        button.isEnabled = !loading
        button.text = if (loading) "Publishing..." else "Publish Event"
    }

    private fun showPublishError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Gagal publish event")
            .setMessage(message.ifBlank { "Event belum bisa dibuat. Silakan coba lagi." })
            .setPositiveButton("OK", null)
            .show()
    }
}

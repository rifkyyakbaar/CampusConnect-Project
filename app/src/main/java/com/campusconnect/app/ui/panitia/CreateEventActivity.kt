package com.campusconnect.app.ui.panitia

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventActivity : AppCompatActivity() {
    private val eventStartCalendar = Calendar.getInstance()
    private var selectedPosterUri: Uri? = null
    private var selectedEventDate = ""
    private var selectedEventTime = ""

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
        setupCategoryDropdown()
        setupEventStartPickers()
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

    private fun setupCategoryDropdown() {
        val categories = listOf("Seminar", "Workshop", "Dies Natalies", "Lainnya")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        findViewById<Spinner>(R.id.spCategory).adapter = adapter
    }

    private fun setupEventStartPickers() {
        val etEventDate = findViewById<EditText>(R.id.etEventDate)
        val etEventTime = findViewById<EditText>(R.id.etEventTime)

        etEventDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    eventStartCalendar.set(Calendar.YEAR, year)
                    eventStartCalendar.set(Calendar.MONTH, month)
                    eventStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedEventDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(eventStartCalendar.time)
                    etEventDate.setText(selectedEventDate)
                },
                eventStartCalendar.get(Calendar.YEAR),
                eventStartCalendar.get(Calendar.MONTH),
                eventStartCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etEventTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    eventStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    eventStartCalendar.set(Calendar.MINUTE, minute)
                    selectedEventTime = SimpleDateFormat("HH:mm", Locale.US).format(eventStartCalendar.time)
                    etEventTime.setText(selectedEventTime)
                },
                eventStartCalendar.get(Calendar.HOUR_OF_DAY),
                eventStartCalendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun publishEvent() {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        val eventName = findViewById<EditText>(R.id.etEventName).text.toString().trim()
        val category = findViewById<Spinner>(R.id.spCategory).selectedItem?.toString().orEmpty()
        val location = findViewById<EditText>(R.id.etLocation).text.toString().trim()
        val capacityText = findViewById<EditText>(R.id.etCapacity).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()
        val ticketPriceText = findViewById<EditText>(R.id.etTicketPrice).text.toString().trim()
        val eventDate = listOf(selectedEventDate, selectedEventTime)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (eventName.isEmpty() || category.isEmpty() || location.isEmpty() || capacityText.isEmpty() || description.isEmpty() || selectedEventDate.isEmpty() || selectedEventTime.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Kapasitas harus berupa angka lebih dari 0", Toast.LENGTH_SHORT).show()
            return
        }

        // Capture ticket price: if empty or 0, treat as free event (0)
        val ticketPrice = if (ticketPriceText.isEmpty()) 0 else ticketPriceText.toIntOrNull() ?: 0

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
        SupabaseRepository.createEvent(this, eventName, category, location, capacity, description, eventDate, selectedPosterUri, ticketPrice) { result ->
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

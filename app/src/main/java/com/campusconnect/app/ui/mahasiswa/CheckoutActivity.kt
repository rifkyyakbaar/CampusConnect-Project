package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.databinding.ActivityCheckoutBinding
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.util.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Locale


class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private var eventCategory: String = ""
    private var eventLocation: String = ""
    private var eventPrice: Int = 0
    private var eventId: String = ""
    private var eventName: String = ""
    private var eventDate: String = ""
    private var isFreeEvent: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Capture intent data
        eventPrice = intent.getIntExtra("eventPrice", 0)
        eventId = intent.getStringExtra("eventId").orEmpty()
        eventName = intent.getStringExtra("eventName").orEmpty()
        eventDate = intent.getStringExtra("eventDate").orEmpty()
        eventCategory = intent.getStringExtra("category").orEmpty()
        eventLocation = intent.getStringExtra("location").orEmpty()

        // Determine if event is free or paid
        isFreeEvent = eventPrice == 0

        // Setup UI based on event type
        setupEventTypeUI()

        // Setup button listeners
        setupListeners()
    }

    private fun setupEventTypeUI() {
        if (isFreeEvent) {
            // Gratis Event
            binding.paymentSection.visibility = View.GONE
            binding.btnConfirmRegister.text = "Get Free Ticket"
        } else {
            // Berbayar Event
            binding.paymentSection.visibility = View.VISIBLE
            binding.btnConfirmRegister.text = "Upload & Confirm"
            binding.tvEventPrice.text = formatPrice(eventPrice)
        }
    }

    private fun formatPrice(price: Int): String {
        return "Rp ${String.format("%,d", price).replace(",", ".")}"
    }

    private fun setupListeners() {
        binding.btnBackCheckout.setOnClickListener {
            finish()
        }

        binding.btnConfirmRegister.setOnClickListener {
            if (isFreeEvent) {
                processRegistration()
            } else {
                uploadPaymentProof()
            }
        }

        binding.btnUploadProof.setOnClickListener {
            uploadPaymentProof()
        }
    }

    private fun uploadPaymentProof() {
        // Dummy function for uploading payment proof
        // In a real implementation, this would:
        // 1. Open a file picker for payment screenshot
        // 2. Upload to Supabase storage
        // 3. Save the proof URL to the database

        Toast.makeText(this, "Opening payment proof uploader...", Toast.LENGTH_SHORT).show()

        // After successful upload, process the registration
        processRegistration()
    }

    private fun processRegistration() {

        SupabaseRepository.createTicket(
            context = this,
            eventId = eventId,
            eventName = eventName,
            category = eventCategory,
            eventDate = eventDate,
            eventLocation = eventLocation
        ) { result ->

            result.onSuccess {

                Toast.makeText(
                    this,
                    "Registration berhasil! Ticket generated.",
                    Toast.LENGTH_SHORT
                ).show()

                scheduleEventReminder()

                startActivity(
                    Intent(this, ManageTicketActivity::class.java)
                )

                finish()
            }

            result.onFailure {

                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun scheduleEventReminder() {
        try {
            // Convert event date string to timestamp
            val eventDateTimestamp = parseEventDateToTimestamp(eventDate)

            // Schedule the reminder only if timestamp is valid (in the future)
            if (eventDateTimestamp > System.currentTimeMillis()) {
                ReminderScheduler.scheduleH1Reminder(
                    this,
                    eventName,
                    eventDateTimestamp
                )
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun parseEventDateToTimestamp(dateString: String): Long {
        return runCatching {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        }.getOrElse { System.currentTimeMillis() }
    }
}
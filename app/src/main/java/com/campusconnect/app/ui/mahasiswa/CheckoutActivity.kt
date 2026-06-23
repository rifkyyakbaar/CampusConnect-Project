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
        eventLocation = intent.getStringExtra("eventLocation").orEmpty()


        setupUI()
        // Setup button listeners
        setupListeners()
    }


    private fun formatPrice(price: Int): String {
        return "Rp ${String.format("%,d", price).replace(",", ".")}"
    }

    private fun setupListeners() {
        binding.btnConfirmRegister.setOnClickListener {
            processRegistration()
        }
    }
    private fun setupUI() {

        binding.paymentSection.visibility = View.GONE

        binding.btnConfirmRegister.text = "Get Ticket"

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
            val user = SupabaseRepository.currentUser(this) ?: return
            val eventDateTimestamp = parseEventDateToTimestamp(eventDate)
            if (eventDateTimestamp > System.currentTimeMillis()) {
                ReminderScheduler.scheduleAllReminders(
                    context            = this,
                    userId             = user.uid,
                    eventId            = eventId,
                    eventName          = eventName,
                    eventDateTimestamp = eventDateTimestamp
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseEventDateToTimestamp(dateString: String): Long {
        return runCatching {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        }.getOrElse { System.currentTimeMillis() }
    }
}
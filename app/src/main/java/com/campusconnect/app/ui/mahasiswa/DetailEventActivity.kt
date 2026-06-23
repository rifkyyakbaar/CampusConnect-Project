package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.utils.setBlinkOnClick

class DetailEventActivity : AppCompatActivity() {

    private lateinit var ivDetailPoster: ImageView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvTicketsLeft: TextView
    private lateinit var btnJoinEvent: Button
    private lateinit var btnBack: ImageView

    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_event)

        eventId = intent.getStringExtra("eventId").orEmpty()

        bindViews()
        setupInitialState()
        loadEvent()
    }

    private fun bindViews() {
        ivDetailPoster      = findViewById(R.id.ivDetailPoster)
        tvDetailCategory    = findViewById(R.id.tvDetailCategory)
        tvDetailDate        = findViewById(R.id.tvDetailDate)
        tvDetailTitle       = findViewById(R.id.tvDetailTitle)
        tvDetailLocation    = findViewById(R.id.tvDetailLocation)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvTicketsLeft       = findViewById(R.id.tvTicketsLeft)
        btnJoinEvent        = findViewById(R.id.btnJoinEvent)
        btnBack             = findViewById(R.id.btnBack)
    }

    private fun setupInitialState() {
        tvDetailCategory.text    = "-"
        tvDetailDate.text        = "-"
        tvDetailTitle.text       = "Loading event..."
        tvDetailLocation.text    = "-"
        tvDetailDescription.text = "-"
        tvTicketsLeft.text       = "-"
        btnJoinEvent.isEnabled   = false
        btnJoinEvent.text        = "Join Event"

        btnBack.setBlinkOnClick { finish() }
    }

    private fun loadEvent() {
        if (eventId.isBlank()) {
            Toast.makeText(this, "Event tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        SupabaseRepository.loadEventById(eventId) { result ->
            result
                .onSuccess { event -> showEvent(event) }
                .onFailure { exception ->
                    Toast.makeText(
                        this,
                        exception.localizedMessage ?: "Gagal memuat detail event.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
        }
    }

    private fun showEvent(event: Event) {
        tvDetailCategory.text    = event.category.ifBlank { "-" }.uppercase()
        tvDetailDate.text        = formatDate(event.eventDate)
        tvDetailTitle.text       = event.eventName.ifBlank { "Untitled Event" }
        tvDetailLocation.text    = event.location.ifBlank { "Lokasi belum ditentukan" }
        tvDetailDescription.text = event.description.ifBlank { "Tidak ada deskripsi event." }

        val ticketsLeft = (event.capacity - event.registrants).coerceAtLeast(0)
        tvTicketsLeft.text = "$ticketsLeft / ${event.capacity}"

        btnJoinEvent.isEnabled = ticketsLeft > 0
        btnJoinEvent.text = if (ticketsLeft > 0) "Join Event" else "Sold Out"

        btnJoinEvent.setBlinkOnClick { joinEvent(event) }

        loadPoster(event.headerImageUrl.ifBlank { event.posterUrl })
    }

    private fun joinEvent(event: Event) {
        // Matikan tombol sementara saat sedang mengecek ke database agar tidak di-spam klik
        btnJoinEvent.isEnabled = false
        val originalText = btnJoinEvent.text
        btnJoinEvent.text = "Mengecek pendaftaran..."

        // Panggil fungsi pengecekan di Supabase
        SupabaseRepository.checkAlreadyRegistered(this, event.id) { result ->
            // Nyalakan kembali tombol
            btnJoinEvent.isEnabled = true
            btnJoinEvent.text = originalText

            result.onSuccess { isAlreadyRegistered ->
                if (isAlreadyRegistered) {
                    // Cegah dan tampilkan peringatan
                    Toast.makeText(
                        this,
                        "Anda sudah mendaftar di event ini! Silakan cek menu My Tickets.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Jika belum terdaftar, arahkan ke proses selanjutnya sesuai tipe pembayaran
                    if (event.paymentType == "FREE") {
                        startActivity(
                            Intent(this, CheckoutActivity::class.java).apply {
                                putExtra("eventId", event.id)
                                putExtra("eventPrice", event.eventPrice)
                                putExtra("eventName", event.eventName)
                                putExtra("eventDate", event.eventDate)
                                putExtra("eventLocation", event.location)
                                putExtra("category", event.category)
                            }
                        )
                    } else {
                        startActivity(
                            Intent(this, PaymentConfirmationActivity::class.java).apply {
                                putExtra("eventId", event.id)
                                putExtra("eventName", event.eventName)
                                putExtra("eventPrice", event.eventPrice)
                                putExtra("paymentInfo", event.paymentInfo)
                                putExtra("eventDate", event.eventDate)
                                putExtra("eventLocation", event.location)
                                putExtra("category", event.category)
                            }
                        )
                    }
                }
            }.onFailure { exception ->
                // Jika internet mati atau gagal ngecek, jangan izinkan lewat untuk cari aman
                Toast.makeText(
                    this,
                    "Gagal mengecek status pendaftaran: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadPoster(posterUrl: String) {
        if (posterUrl.isBlank()) return
        Glide.with(this)
            .load(posterUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .centerCrop()
            .into(ivDetailPoster)
    }

    private fun formatDate(rawDate: String?): String {
        val value = rawDate.orEmpty()
        return if (value.length >= 10) value.substring(0, 10) else "-"
    }
}

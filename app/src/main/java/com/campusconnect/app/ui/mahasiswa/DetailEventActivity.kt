package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.ui.panitia.ManagePesertaActivity
import java.net.URL


class DetailEventActivity : AppCompatActivity() {
    private lateinit var ivDetailPoster: ImageView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvTicketsLeft: TextView
    private lateinit var btnJoinEvent: Button
    private var eventId: String = ""
    private var source: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_event)

        eventId = intent.getStringExtra("eventId").orEmpty()
        source = intent.getStringExtra("source").orEmpty()

        bindViews()
        setupInitialState()
        loadEvent()
    }

    private fun bindViews() {
        ivDetailPoster = findViewById(R.id.ivDetailPoster)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailDate = findViewById(R.id.tvDetailDate)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailLocation = findViewById(R.id.tvDetailLocation)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvTicketsLeft = findViewById(R.id.tvTicketsLeft)
        btnJoinEvent = findViewById(R.id.btnJoinEvent)
    }

    private fun setupInitialState() {
        tvDetailCategory.text = "-"
        tvDetailDate.text = "-"
        tvDetailTitle.text = "Loading event..."
        tvDetailLocation.text = "-"
        tvDetailDescription.text = "-"
        tvTicketsLeft.text = "-"
        btnJoinEvent.isEnabled = false
        btnJoinEvent.text = if (source.equals("panitia", ignoreCase = true)) {
            "Manage Participants"
        } else {
            "Join Event"
        }
    }

    private fun loadEvent() {
        if (eventId.isBlank()) {
            Toast.makeText(this, "Event tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        SupabaseRepository.loadEventById(eventId) { result ->
            result
                .onSuccess { event ->
                    showEvent(event)
                }
                .onFailure { exception ->
                    Toast.makeText(this, exception.localizedMessage ?: "Gagal memuat detail event.", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun showEvent(event: Event) {
        tvDetailCategory.text = event.category.ifBlank { "-" }.uppercase()
        tvDetailDate.text = formatDate(event.createdAt)
        tvDetailTitle.text = event.eventName.ifBlank { "Untitled Event" }
        tvDetailLocation.text = event.location.ifBlank { "Lokasi belum ditentukan" }
        tvDetailDescription.text = event.description.ifBlank { "Tidak ada deskripsi event." }

        val ticketsLeft = (event.capacity - event.registrants).coerceAtLeast(0)
        tvTicketsLeft.text = "$ticketsLeft / ${event.capacity}"
        btnJoinEvent.isEnabled = true
        btnJoinEvent.setOnClickListener {
            openAction(event)
        }

        loadPoster(event.posterUrl)
    }

    private fun openAction(event: Event) {
        if (source.equals("panitia", ignoreCase = true)) {
            startActivity(Intent(this, ManagePesertaActivity::class.java).apply {
                putExtra("eventId", event.id)
            })
        } else {
            startActivity(Intent(this, CheckoutActivity::class.java).apply {
                putExtra("eventId", event.id)
            })
        }
    }

    private fun loadPoster(posterUrl: String) {
        if (posterUrl.isBlank()) return

        Thread {
            val bitmap = runCatching {
                URL(posterUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()

            if (bitmap != null) {
                runOnUiThread {
                    ivDetailPoster.setImageBitmap(bitmap)
                }
            }
        }.start()
    }

    private fun formatDate(rawDate: String?): String {
        val value = rawDate.orEmpty()
        return if (value.length >= 10) value.substring(0, 10) else "-"
    }
}

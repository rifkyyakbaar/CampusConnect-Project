package com.campusconnect.app.ui.panitia

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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.net.URL

class DetailPanitiaEventActivity : AppCompatActivity() {

    private lateinit var ivDetailPoster: ImageView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvTicketsLeft: TextView

    private lateinit var btnManageParticipants: Button
    private lateinit var btnScanTicket: FloatingActionButton   // ← FAB bukan Button

    private var eventId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_panitia_event)

        eventId = intent.getStringExtra("eventId").orEmpty()

        bindViews()
        loadEvent()
    }

    private fun bindViews() {
        ivDetailPoster        = findViewById(R.id.ivDetailPoster)
        tvDetailCategory      = findViewById(R.id.tvDetailCategory)
        tvDetailDate          = findViewById(R.id.tvDetailDate)
        tvDetailTitle         = findViewById(R.id.tvDetailTitle)
        tvDetailLocation      = findViewById(R.id.tvDetailLocation)
        tvDetailDescription   = findViewById(R.id.tvDetailDescription)
        tvTicketsLeft         = findViewById(R.id.tvTicketsLeft)
        btnManageParticipants = findViewById(R.id.btnManageParticipants)
        btnScanTicket         = findViewById(R.id.btnScanTicket)
    }

    private fun loadEvent() {
        SupabaseRepository.loadEventById(eventId) { result ->
            result
                .onSuccess { event -> showEvent(event) }
                .onFailure {
                    Toast.makeText(this, "Gagal memuat event", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun showEvent(event: Event) {
        tvDetailCategory.text  = event.category.uppercase()
        tvDetailDate.text      = event.eventDate
        tvDetailTitle.text     = event.eventName
        tvDetailLocation.text  = event.location
        tvDetailDescription.text = event.description

        val ticketsLeft = (event.capacity - event.registrants).coerceAtLeast(0)
        tvTicketsLeft.text = "$ticketsLeft / ${event.capacity}"

        btnManageParticipants.setOnClickListener {
            startActivity(
                Intent(this, ManagePesertaActivity::class.java).apply {
                    putExtra("eventId", event.id)
                }
            )
        }

        btnScanTicket.setOnClickListener {
            startActivity(
                Intent(this, ScannerActivity::class.java).apply {
                    putExtra("eventId", event.id)
                    putExtra("eventName", event.eventName)
                }
            )
        }

        loadPoster(event.posterUrl)
    }

    private fun loadPoster(posterUrl: String) {
        if (posterUrl.isBlank()) return
        Thread {
            val bitmap = runCatching {
                URL(posterUrl).openStream().use { BitmapFactory.decodeStream(it) }
            }.getOrNull()
            if (bitmap != null) {
                runOnUiThread { ivDetailPoster.setImageBitmap(bitmap) }
            }
        }.start()
    }
}
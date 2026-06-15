package com.campusconnect.app.ui.admin

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class VerifyEventActivity : AppCompatActivity() {

    private lateinit var imgPoster: ImageView
    private lateinit var tvOrganizer: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvCapacity: TextView
    private lateinit var tvDescription: TextView

    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button

    private var eventId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_event)

        imgPoster = findViewById(R.id.imgVerifyPoster)
        tvOrganizer = findViewById(R.id.tvVerifyOrganizer)
        tvTitle = findViewById(R.id.tvVerifyTitle)
        tvCategory = findViewById(R.id.tvVerifyCategory)
        tvLocation = findViewById(R.id.tvVerifyLocation)
        tvCapacity = findViewById(R.id.tvVerifyCapacity)
        tvDescription = findViewById(R.id.tvVerifyDesc)

        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)

        eventId = intent.getStringExtra("eventId") ?: ""

        loadEvent()

        btnApprove.setOnClickListener {
            approveEvent()
        }

        btnReject.setOnClickListener {
            rejectEvent()
        }
    }

    private fun loadEvent() {

        SupabaseRepository.loadEventById(eventId) { result ->

            result.onSuccess { event ->

                tvOrganizer.text = event.organizerName
                tvTitle.text = event.eventName
                tvCategory.text = "Category : ${event.category}"
                tvLocation.text = "Location : ${event.location}"
                tvCapacity.text = "Capacity : ${event.capacity}"
                tvDescription.text = event.description

                Glide.with(this)
                    .load(event.posterUrl)
                    .placeholder(R.drawable.logo_campus_connect)
                    .into(imgPoster)

            }.onFailure {
                Toast.makeText(
                    this,
                    "Event tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun approveEvent() {

        SupabaseRepository.updateEventStatus(
            eventId,
            "approved"
        ) { result ->

            result.onSuccess {

                Toast.makeText(
                    this,
                    "Event berhasil disetujui",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            }.onFailure {

                Toast.makeText(
                    this,
                    "Gagal menyetujui event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejectEvent() {

        SupabaseRepository.updateEventStatus(
            eventId,
            "rejected"
        ) { result ->

            result.onSuccess {

                Toast.makeText(
                    this,
                    "Event ditolak",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            }.onFailure {

                Toast.makeText(
                    this,
                    "Gagal menolak event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
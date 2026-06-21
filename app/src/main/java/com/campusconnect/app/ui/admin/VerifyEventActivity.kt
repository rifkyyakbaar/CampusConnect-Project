package com.campusconnect.app.ui.admin

import android.graphics.Color
import android.os.Bundle
import android.view.View
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
    private lateinit var tvEventDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var bottomVerifyBar: View
    private lateinit var tvVerifyPrice: TextView
    private lateinit var tvVerifyPaymentType: TextView
    private lateinit var tvVerifyPaymentInfo: TextView

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
        tvEventDate = findViewById(R.id.tvVerifyEventDate)
        tvStatus = findViewById(R.id.tvVerifyStatus)
        tvDescription = findViewById(R.id.tvVerifyDesc)
        tvVerifyPrice = findViewById(R.id.tvVerifyPrice)
        tvVerifyPaymentType = findViewById(R.id.tvVerifyPaymentType)
        tvVerifyPaymentInfo = findViewById(R.id.tvVerifyPaymentInfo)
        bottomVerifyBar = findViewById(R.id.bottomVerifyBar)

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
                tvVerifyPrice.text =
                    if (event.eventPrice == 0)
                        "Price : FREE"
                    else
                        "Price : Rp ${event.eventPrice}"

                tvVerifyPaymentType.text =
                    "Payment Type : ${event.paymentType}"

                tvVerifyPaymentInfo.text =
                    if (event.paymentType == "PAID")
                        "Payment Info : ${event.paymentInfo}"
                    else
                        ""
                tvEventDate.text = "Start : ${event.eventDate.ifBlank { "-" }}"
                showStatus(event.status)
                tvDescription.text = event.description

                Glide.with(this)
                    .load(event.headerImageUrl.ifBlank { event.posterUrl })
                    .placeholder(R.drawable.logo_campus_connect)
                    .centerCrop()
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
        setActionLoading(true)

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

                showStatus("approved")
                finish()

            }.onFailure {
                setActionLoading(false)

                Toast.makeText(
                    this,
                    "Gagal menyetujui event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejectEvent() {
        setActionLoading(true)

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

                showStatus("rejected")
                finish()

            }.onFailure {
                setActionLoading(false)

                Toast.makeText(
                    this,
                    "Gagal menolak event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showStatus(statusValue: String) {
        val status = statusValue.ifBlank { "pending" }
        tvStatus.text = status.uppercase()
        tvStatus.setTextColor(statusColor(status))

        val canReview = status.equals("pending", ignoreCase = true)
        bottomVerifyBar.visibility = if (canReview) View.VISIBLE else View.GONE
        btnApprove.isEnabled = canReview
        btnReject.isEnabled = canReview
    }

    private fun setActionLoading(isLoading: Boolean) {
        btnApprove.isEnabled = !isLoading
        btnReject.isEnabled = !isLoading
    }

    private fun statusColor(status: String): Int {
        return when {
            status.equals("approved", ignoreCase = true) -> Color.rgb(22, 163, 74)
            status.equals("rejected", ignoreCase = true) -> Color.rgb(220, 38, 38)
            else -> Color.rgb(245, 158, 11)
        }
    }
}

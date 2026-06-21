package com.campusconnect.app.ui.mahasiswa

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository

class ReviewActivity : AppCompatActivity() {

    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var btnSubmitReview: Button
    private lateinit var btnCloseReview: ImageView

    private var ticketId = ""
    private var eventId  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        ticketId = intent.getStringExtra("ticketId").orEmpty()
        eventId  = intent.getStringExtra("eventId").orEmpty()

        ratingBar       = findViewById(R.id.ratingBar)
        etComment       = findViewById(R.id.etComment)
        btnSubmitReview = findViewById(R.id.btnSubmitReview)
        btnCloseReview  = findViewById(R.id.btnCloseReview)

        btnCloseReview.setOnClickListener { finish() }

        btnSubmitReview.setOnClickListener { submitReview() }
    }

    private fun submitReview() {
        val rating  = ratingBar.rating.toInt()
        val comment = etComment.text.toString().trim()

        if (rating == 0) {
            Toast.makeText(this, "Pilih rating terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmitReview.isEnabled = false
        btnSubmitReview.text = "Menyimpan..."

        SupabaseRepository.createReview(this, ticketId, eventId, rating, comment) { result ->
            result
                .onSuccess {
                    AlertDialog.Builder(this)
                        .setTitle("Terima Kasih!")
                        .setMessage("Review kamu berhasil disimpan.")
                        .setPositiveButton("OK") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
                .onFailure { e ->
                    btnSubmitReview.isEnabled = true
                    btnSubmitReview.text = "Submit Review"
                    Toast.makeText(this, e.message ?: "Gagal menyimpan review", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

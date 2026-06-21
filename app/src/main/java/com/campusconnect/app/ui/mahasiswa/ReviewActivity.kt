package com.campusconnect.app.ui.mahasiswa

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R

class ReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val btnCloseReview = findViewById<ImageView>(R.id.btnCloseReview)
        val btnSubmitReview = findViewById<Button>(R.id.btnSubmitReview)

        btnCloseReview.setOnClickListener {
            finish()
        }

        btnSubmitReview.setOnClickListener {
            // Menggunakan MaterialAlertDialogBuilder sesuai request terbaru
            MaterialAlertDialogBuilder(this)
                .setTitle("Sukses")
                .setMessage("Terima Kasih Telah Mengisi Survey")
                .setIcon(android.R.drawable.checkbox_on_background)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
}

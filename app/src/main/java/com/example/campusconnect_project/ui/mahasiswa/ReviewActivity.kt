package com.example.campusconnect_project.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import com.example.campusconnect_project.ui.auth.LoginActivity


class ReviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val btnCloseReview = findViewById<ImageView>(R.id.btnCloseReview)
        val btnSubmitReview = findViewById<Button>(R.id.btnSubmitReview)

        btnCloseReview.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HistoryActivity::class.java
                )
            )
        }

        btnSubmitReview.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HomeMahasiswaActivity::class.java
                )
            )
        }

    }
}
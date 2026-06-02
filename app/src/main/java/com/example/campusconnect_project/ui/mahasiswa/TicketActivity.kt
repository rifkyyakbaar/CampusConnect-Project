package com.example.campusconnect_project.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R


class TicketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket)

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    HomeMahasiswaActivity::class.java
                )
            )
        }
    }
}
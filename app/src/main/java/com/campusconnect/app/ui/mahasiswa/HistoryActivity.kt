package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.campusconnect.app.ui.profile.ProfileActivity


class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val btnReview = findViewById<Button>(R.id.btnReview)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_history

        btnReview.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ReviewActivity::class.java
                )
            )
        }

        bottomNavigation.setOnItemSelectedListener { item ->

            when(item.itemId){

                R.id.nav_home -> {
                    startActivity(
                        Intent(
                            this,
                            HomeMahasiswaActivity::class.java
                        )
                    )
                    true
                }

                R.id.nav_ticket -> {

                    startActivity(
                        Intent(
                            this,
                            TicketActivity::class.java
                        )
                    )

                    true
                }

                R.id.nav_history -> {
                    true
                }

                R.id.nav_profile -> {

                    startActivity(
                        Intent(
                            this,
                            ProfileActivity::class.java
                        )
                    )

                    true
                }

                else -> false
            }
        }
    }
}
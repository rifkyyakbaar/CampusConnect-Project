package com.campusconnect.app.ui.mahasiswa

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.NotificationAdapter
import com.campusconnect.app.data.SupabaseRepository

class NotificationActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var progressNotif: ProgressBar
    private lateinit var btnBack: View
    private lateinit var tvMarkAllRead: TextView

    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        rvNotifications = findViewById(R.id.rvNotifications)
        layoutEmpty      = findViewById(R.id.layoutEmpty)
        progressNotif    = findViewById(R.id.progressNotif)
        btnBack          = findViewById(R.id.btnBack)
        tvMarkAllRead    = findViewById(R.id.tvMarkAllRead)

        setupRecyclerView()
        setupListeners()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notif ->
            // Tandai sebagai sudah dibaca saat item diklik
            if (!notif.isRead) {
                SupabaseRepository.markNotificationRead(this, notif.notificationId) { }
            }
        }
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        tvMarkAllRead.setOnClickListener {
            SupabaseRepository.markAllNotificationsRead(this) { result ->
                result.onSuccess {
                    Toast.makeText(this, "Semua notifikasi ditandai dibaca", Toast.LENGTH_SHORT).show()
                    loadNotifications()
                }.onFailure {
                    Toast.makeText(this, "Gagal menandai notifikasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadNotifications() {
        showLoading(true)

        SupabaseRepository.loadNotifications(this) { result ->
            showLoading(false)

            result.onSuccess { list ->
                if (list.isEmpty()) {
                    showEmpty(true)
                } else {
                    showEmpty(false)
                    rvNotifications.visibility = View.VISIBLE
                    adapter.submitList(list)
                }
            }.onFailure {
                showEmpty(true)
                Toast.makeText(
                    this,
                    "Gagal memuat notifikasi: ${it.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        progressNotif.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            rvNotifications.visibility = View.GONE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun showEmpty(empty: Boolean) {
        layoutEmpty.visibility      = if (empty) View.VISIBLE else View.GONE
        rvNotifications.visibility  = if (empty) View.GONE    else View.VISIBLE
    }
}
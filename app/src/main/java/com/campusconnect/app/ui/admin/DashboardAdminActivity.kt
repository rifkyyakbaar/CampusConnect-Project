package com.campusconnect.app.ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.EventAdminAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var rvAdminApprovals: RecyclerView
    private lateinit var tvPendingApprovalMessage: TextView

    // Semua event dari server (tidak pernah diubah kecuali saat reload)
    private val allEvents = mutableListOf<Event>()
    // Event yang sedang ditampilkan (hasil filter)
    private val displayList = mutableListOf<Event>()
    private lateinit var adapter: EventAdminAdapter

    // Filter pills
    private lateinit var pillPending: CardView
    private lateinit var pillApproved: CardView
    private lateinit var pillRejected: CardView
    private lateinit var tvPillPending: TextView
    private lateinit var tvPillApproved: TextView
    private lateinit var tvPillRejected: TextView

    private var selectedEventStatus = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        rvAdminApprovals = findViewById(R.id.rvAdminApprovals)
        tvPendingApprovalMessage = findViewById(R.id.tvPendingApprovalMessage)

        pillPending  = findViewById(R.id.pillPending)
        pillApproved = findViewById(R.id.pillApproved)
        pillRejected = findViewById(R.id.pillRejected)
        tvPillPending  = findViewById(R.id.tvPillPending)
        tvPillApproved = findViewById(R.id.tvPillApproved)
        tvPillRejected = findViewById(R.id.tvPillRejected)

        // Adapter menunjuk ke displayList — kita update displayList lalu notify
        adapter = EventAdminAdapter(displayList) { event ->
            val intent = Intent(this, VerifyEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }

        rvAdminApprovals.layoutManager = LinearLayoutManager(this)
        rvAdminApprovals.adapter = adapter

        setupPillListeners()
        setupBottomNavigation()
        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun setupPillListeners() {
        pillPending.setOnClickListener {
            selectedEventStatus = "pending"
            applyStatusFilter()
        }
        pillApproved.setOnClickListener {
            selectedEventStatus = "approved"
            applyStatusFilter()
        }
        pillRejected.setOnClickListener {
            selectedEventStatus = "rejected"
            applyStatusFilter()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_manage_event
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_manage_event -> true
                R.id.nav_manage_user -> {
                    startActivity(Intent(this, ManageUserActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadEvents() {
        SupabaseRepository.loadAdminEvents { result ->
            result.onSuccess { events ->
                allEvents.clear()
                allEvents.addAll(events)
                // Kartu kuning selalu dari data global, bukan hasil filter
                updatePendingMessage(allEvents.count { it.status.equals("pending", ignoreCase = true) })
                applyStatusFilter()
            }
        }
    }

    private fun applyStatusFilter() {
        displayList.clear()
        displayList.addAll(
            allEvents.filter { it.status.equals(selectedEventStatus, ignoreCase = true) }
        )
        adapter.notifyDataSetChanged()
        updatePillStates()
    }

    private fun updatePillStates() {
        // Reset semua ke nonaktif
        setPillInactive(pillPending,  tvPillPending)
        setPillInactive(pillApproved, tvPillApproved)
        setPillInactive(pillRejected, tvPillRejected)

        // Aktifkan pill yang dipilih dengan warna semantik masing-masing
        when (selectedEventStatus) {
            "pending" -> {
                pillPending.setCardBackgroundColor(Color.parseColor("#FEF3C7")) // bg_warning
                tvPillPending.setTextColor(Color.parseColor("#92400E"))         // teks_warning
            }
            "approved" -> {
                pillApproved.setCardBackgroundColor(Color.parseColor("#10B981")) // bg_terima
                tvPillApproved.setTextColor(Color.WHITE)
            }
            "rejected" -> {
                pillRejected.setCardBackgroundColor(Color.parseColor("#FEE2E2")) // bg_tolak
                tvPillRejected.setTextColor(Color.parseColor("#DC2626"))         // teks_tolak
            }
        }
    }

    private fun setPillInactive(pill: CardView, tv: TextView) {
        pill.setCardBackgroundColor(Color.parseColor("#30FFFFFF"))
        tv.setTextColor(Color.WHITE)
    }

    private fun updatePendingMessage(pendingCount: Int) {
        tvPendingApprovalMessage.text = when (pendingCount) {
            0 -> "No events waiting for approval."
            1 -> "You have 1 event waiting for approval."
            else -> "You have $pendingCount events waiting for approval."
        }
    }
}
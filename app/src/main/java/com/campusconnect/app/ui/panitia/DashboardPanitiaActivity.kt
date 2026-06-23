package com.campusconnect.app.ui.panitia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.EventPanitiaAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.ui.mahasiswa.NotificationActivity
import com.campusconnect.app.ui.profile.ProfileActivity

class DashboardPanitiaActivity : AppCompatActivity() {

    private val allEvents = mutableListOf<Event>()
    private val myEvents  = mutableListOf<Event>()

    private lateinit var adapter: EventPanitiaAdapter
    private lateinit var rvPanitiaEvents: RecyclerView

    private var selectedFilter = "all"

    private lateinit var btnFilterAll:      CardView
    private lateinit var btnFilterWaiting:  CardView
    private lateinit var btnFilterApproved: CardView
    private lateinit var btnFilterRejected: CardView
    private lateinit var btnFilterFinished: CardView

    // Bell + badge
    private lateinit var frameBellPanitia:     View
    private lateinit var tvNotifBadgePanitia:  TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_panitia)

        setupMyEventsList()
        setupFilterNavbar()
        setupTopIcons()
        loadPanitiaName()
        createevent()
        showDefaultStats()
    }

    override fun onStart() {
        super.onStart()
        loadMyEventsStats()
        loadMyManagedEvents()
        // Refresh badge setiap kali dashboard aktif
        // (termasuk setelah kembali dari NotificationActivity)
        refreshNotifBadge()
    }

    // ─────────────────────────────────────────
    // Bell icon & badge
    // ─────────────────────────────────────────

    private fun setupTopIcons() {
        frameBellPanitia    = findViewById(R.id.frameBellPanitia)
        tvNotifBadgePanitia = findViewById(R.id.tvNotifBadgePanitia)

        frameBellPanitia.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        // Tombol profile di samping bell
        findViewById<ImageButton>(R.id.btnOpenProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun refreshNotifBadge() {
        SupabaseRepository.getUnreadNotificationCount(this) { result ->
            val count = result.getOrDefault(0)
            if (count > 0) {
                tvNotifBadgePanitia.visibility = View.VISIBLE
                tvNotifBadgePanitia.text = if (count > 99) "99+" else count.toString()
            } else {
                tvNotifBadgePanitia.visibility = View.GONE
            }
        }
    }

    // ─────────────────────────────────────────
    // Event list
    // ─────────────────────────────────────────

    private fun setupMyEventsList() {
        rvPanitiaEvents = findViewById(R.id.rvPanitiaEvents)
        adapter = EventPanitiaAdapter(
            eventList = myEvents,
            onDetailClick = { event ->
                startActivity(Intent(this, DetailPanitiaEventActivity::class.java).apply {
                    putExtra("eventId", event.id)
                    putExtra("source", "panitia")
                })
            },
            onEditClick = { event ->
                startActivity(Intent(this, CreateEventActivity::class.java).apply {
                    putExtra("mode", "edit")
                    putExtra("eventId", event.id)
                })
            }
        )
        rvPanitiaEvents.layoutManager = LinearLayoutManager(this)
        rvPanitiaEvents.adapter = adapter
    }

    // ─────────────────────────────────────────
    // Filter navbar
    // ─────────────────────────────────────────

    private fun setupFilterNavbar() {
        btnFilterAll      = findViewById(R.id.btnFilterAll)
        btnFilterWaiting  = findViewById(R.id.btnFilterWaiting)
        btnFilterApproved = findViewById(R.id.btnFilterApproved)
        btnFilterRejected = findViewById(R.id.btnFilterRejected)
        btnFilterFinished = findViewById(R.id.btnFilterFinished)

        btnFilterAll.setOnClickListener      { selectFilter("all") }
        btnFilterWaiting.setOnClickListener  { selectFilter("waiting") }
        btnFilterApproved.setOnClickListener { selectFilter("approved") }
        btnFilterRejected.setOnClickListener { selectFilter("rejected") }
        btnFilterFinished.setOnClickListener { selectFilter("finished") }

        updateFilterVisualState()
    }

    private fun selectFilter(filter: String) {
        if (selectedFilter == filter) return
        selectedFilter = filter
        updateFilterVisualState()
        applyFilter()
    }

    private fun updateFilterVisualState() {
        val activeColor = when (selectedFilter) {
            "waiting"  -> getColor(R.color.bg_warning)
            "approved" -> getColor(R.color.bg_terima)
            "rejected" -> getColor(R.color.bg_tolak)
            "finished" -> getColor(R.color.teks_sekunder)
            else       -> getColor(R.color.warna_primer)
        }
        val activeTextColor = when (selectedFilter) {
            "waiting"  -> getColor(R.color.teks_warning)
            "rejected" -> getColor(R.color.teks_tolak)
            else       -> getColor(R.color.black)
        }

        val inactiveBg   = android.graphics.Color.parseColor("#30FFFFFF")
        val inactiveText = getColor(R.color.white)

        val pills = mapOf(
            "all"      to btnFilterAll,
            "waiting"  to btnFilterWaiting,
            "approved" to btnFilterApproved,
            "rejected" to btnFilterRejected,
            "finished" to btnFilterFinished
        )

        pills.forEach { (key, card) ->
            val label = card.getChildAt(0) as? TextView
            if (key == selectedFilter) {
                card.setCardBackgroundColor(activeColor)
                label?.setTextColor(activeTextColor)
            } else {
                card.setCardBackgroundColor(inactiveBg)
                label?.setTextColor(inactiveText)
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (selectedFilter) {
            "waiting"  -> allEvents.filter {
                it.status.equals("pending", ignoreCase = true)
            }
            "approved" -> allEvents.filter {
                it.status.equals("approved", ignoreCase = true) &&
                        !EventPanitiaAdapter.isEventFinished(it)
            }
            "rejected" -> allEvents.filter {
                it.status.equals("rejected", ignoreCase = true)
            }
            "finished" -> allEvents.filter {
                it.status.equals("approved", ignoreCase = true) &&
                        EventPanitiaAdapter.isEventFinished(it)
            }
            else -> allEvents.toList()
        }

        myEvents.clear()
        myEvents.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateLabel(filtered.isEmpty())
    }

    private fun updateLabel(isEmpty: Boolean) {
        val label = findViewById<TextView>(R.id.tvMyEventsLabel)
        label.text = if (isEmpty) "My Managed Events - No events yet" else "My Managed Events"
    }

    // ─────────────────────────────────────────
    // Load data
    // ─────────────────────────────────────────

    private fun loadPanitiaName() {
        val tvPanitiaName = findViewById<TextView>(R.id.tvPanitiaName)
        val user = SupabaseRepository.currentUser(this)

        if (user == null) {
            tvPanitiaName.text = "Hello, Pengguna!"
            return
        }

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            val fullName = result.getOrNull()?.fullName
                ?: user.fullName.ifBlank { user.email.substringBefore("@").ifBlank { "Pengguna" } }
            tvPanitiaName.text = "Hello, $fullName!"
        }
    }

    private fun createevent() {
        val fabAddEvent = findViewById<ImageButton>(R.id.fabAddEvent)
        fabAddEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
    }

    private fun showDefaultStats() {
        findViewById<TextView>(R.id.tvTotalEvents).text     = "0"
        findViewById<TextView>(R.id.tvTotalRegistrants).text = "0"
    }

    private fun loadMyEventsStats() {
        val user = SupabaseRepository.currentUser(this) ?: return
        SupabaseRepository.loadOrganizerStats(user.uid) { result ->
            result.onSuccess { stats ->
                findViewById<TextView>(R.id.tvTotalEvents).text     = stats.first.toString()
                findViewById<TextView>(R.id.tvTotalRegistrants).text = stats.second.toString()
            }.onFailure {
                showDefaultStats()
            }
        }
    }

    private fun loadMyManagedEvents() {
        val user = SupabaseRepository.currentUser(this) ?: run {
            allEvents.clear()
            applyFilter()
            return
        }

        SupabaseRepository.loadOrganizerEvents(user.uid) { result ->
            result.onSuccess { events ->
                allEvents.clear()
                allEvents.addAll(events)
                applyFilter()
            }.onFailure { exception ->
                allEvents.clear()
                applyFilter()
                Toast.makeText(
                    this,
                    exception.localizedMessage ?: "Gagal memuat event.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
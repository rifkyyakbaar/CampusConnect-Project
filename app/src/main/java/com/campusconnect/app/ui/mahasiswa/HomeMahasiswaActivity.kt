package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.databinding.ActivityHomeMahasiswaBinding
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.ui.profile.ProfileActivity
import java.text.SimpleDateFormat
import java.util.Locale

class HomeMahasiswaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeMahasiswaBinding
    private lateinit var adapter: EventMahasiswaAdapter

    // Master list and filtered list
    private val allEvents = mutableListOf<Event>()
    private val filteredEvents = mutableListOf<Event>()

    // Filter state variables
    private var currentSearchQuery = ""
    private var currentCategory = "All"
    private var isSortAscending = true

    // Category buttons for easy reference
    private lateinit var categoryButtons: Map<String, Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMahasiswaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView with adapter
        adapter = EventMahasiswaAdapter(filteredEvents) { event ->
            val intent = Intent(this, DetailEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            intent.putExtra("eventPrice", event.eventPrice)
            startActivity(intent)
        }

        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = adapter

        // Initialize category buttons map
        setupCategoryButtons()

        // Setup listeners
        setupSearchListener()
        setupCategoryListeners()
        setupSortListener()

        // Load data
        loadWelcomeName()
        loadEvents()
        setupBottomNavigation()
    }

    private fun setupCategoryButtons() {
        categoryButtons = mapOf(
            "All" to binding.btnCategoryAll,
            "Seminar" to binding.btnCategorySeminar,
            "Workshop" to binding.btnCategoryWorkshop,
            "Dies Natalies" to binding.btnCategoryDiesNatalies,
            "Lainnya" to binding.btnCategoryLainnya
        )
    }

    private fun setupSearchListener() {
        binding.etSearchEvent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategoryListeners() {
        categoryButtons.forEach { (categoryName, button) ->
            button.setOnClickListener {
                currentCategory = categoryName
                updateCategoryButtonStates()
                applyFilters()
            }
        }
    }

    private fun updateCategoryButtonStates() {
        categoryButtons.forEach { (categoryName, button) ->
            if (categoryName == currentCategory) {
                // Active button - filled with primary color
                button.setBackgroundColor(resources.getColor(R.color.warna_primer, null))
                button.setTextColor(resources.getColor(R.color.white, null))
            } else {
                // Inactive button - outlined style
                button.setBackgroundColor(resources.getColor(R.color.bg_utama, null))
                button.setTextColor(resources.getColor(R.color.teks_sekunder, null))
            }
        }
    }

    private fun setupSortListener() {
        binding.cvSortButton.setOnClickListener {
            isSortAscending = !isSortAscending
            // Add rotation animation
            binding.cvSortButton.animate()
                .rotationBy(180f)
                .setDuration(300)
                .start()
            applyFilters()
        }
    }

    private fun applyFilters() {
        // Step 1: Filter by search query
        var results = allEvents.filter { event ->
            event.eventName.contains(currentSearchQuery, ignoreCase = true)
        }.toMutableList()

        // Step 2: Filter by category
        if (currentCategory != "All") {
            results = results.filter { event ->
                event.category.equals(currentCategory, ignoreCase = true)
            }.toMutableList()
        }

        // Step 3: Sort by event date
        results.sortWith(compareBy { event ->
            parseEventDate(event.eventDate)
        })

        if (!isSortAscending) {
            results.reverse()
        }

        // Step 4: Update the filtered list and refresh adapter
        filteredEvents.clear()
        filteredEvents.addAll(results)
        adapter.notifyDataSetChanged()

        // Step 5: Handle empty state
        if (filteredEvents.isEmpty()) {
            binding.tvEmptyEvent.visibility = View.VISIBLE
            binding.rvEvents.visibility = View.GONE
        } else {
            binding.tvEmptyEvent.visibility = View.GONE
            binding.rvEvents.visibility = View.VISIBLE
        }
    }

    private fun parseEventDate(dateString: String): Long {
        return runCatching {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            format.parse(dateString)?.time ?: 0L
        }.getOrElse { 0L }
    }

    private fun loadWelcomeName() {
        val user = SupabaseRepository.currentUser(this)

        if (user == null) {
            binding.tvWelcomeName.text = getString(R.string.welcome_user, "Pengguna")
            return
        }

        SupabaseRepository.loadUserProfile(this, user.uid) { result ->
            val fullName = result.getOrNull()?.fullName
                ?: user.fullName.ifBlank {
                    user.email.substringBefore("@").ifBlank { "Pengguna" }
                }

            binding.tvWelcomeName.text =
                getString(R.string.welcome_user, fullName)
        }
    }

    private fun loadEvents() {
        SupabaseRepository.loadApprovedEvents { result ->
            result.onSuccess { events ->
                allEvents.clear()
                allEvents.addAll(events)
                // Initialize filtered list with all events
                applyFilters()
            }

            result.onFailure {
                // optional Toast
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> true

                R.id.nav_ticket -> {
                    startActivity(
                        Intent(this, ManageTicketActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_history -> {
                    startActivity(
                        Intent(this, HistoryActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }
}
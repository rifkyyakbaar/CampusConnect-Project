package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.databinding.ActivityCategoryEventsBinding
import com.campusconnect.app.model.Event
import com.campusconnect.app.utils.setBlinkOnClick

class CategoryEventsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryEventsBinding
    private lateinit var adapter: CategoryEventsAdapter
    private var isGridView = true
    private var allCategoryEvents = mutableListOf<Event>()
    private var filteredEvents = mutableListOf<Event>()
    
    private var isDateAscending = true
    private var isPriceAscending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryEventsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val category = intent.getStringExtra("category") ?: "All"
        binding.tvCategoryTitle.text = if (category == "All") "All Events" else category

        setupRecyclerView()
        setupListeners()
        loadEvents(category)
    }

    private fun setupRecyclerView() {
        adapter = CategoryEventsAdapter { event ->
            val intent = Intent(this, DetailEventActivity::class.java).apply {
                putExtra("eventId", event.id)
                putExtra("eventPrice", event.eventPrice)
            }
            startActivity(intent)
        }
        binding.rvCategoryEvents.layoutManager = GridLayoutManager(this, 2)
        binding.rvCategoryEvents.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setBlinkOnClick { finish() }

        binding.btnToggleLayout.setBlinkOnClick {
            toggleLayout()
        }

        binding.btnSearch.setBlinkOnClick {
            showSearchBar()
        }

        binding.btnCancelSearch.setBlinkOnClick {
            hideSearchBar()
        }

        binding.btnSortDate.setBlinkOnClick {
            isDateAscending = !isDateAscending
            sortEventsByDate()
        }

        binding.btnSortPrice.setBlinkOnClick {
            isPriceAscending = !isPriceAscending
            sortEventsByPrice()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterEvents(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun toggleLayout() {
        isGridView = !isGridView
        val spanCount = if (isGridView) 2 else 1
        (binding.rvCategoryEvents.layoutManager as GridLayoutManager).spanCount = spanCount
        adapter.setViewType(isGridView)
        
        val iconRes = if (isGridView) android.R.drawable.ic_dialog_dialer else android.R.drawable.ic_menu_sort_by_size
        binding.btnToggleLayout.setImageResource(iconRes)
    }

    private fun showSearchBar() {
        TransitionManager.beginDelayedTransition(binding.headerRoot as ViewGroup)
        binding.tvCategoryTitle.visibility = View.GONE
        binding.btnSearch.visibility = View.GONE
        binding.cvSearchBar.visibility = View.VISIBLE
        binding.etSearch.requestFocus()
    }

    private fun hideSearchBar() {
        TransitionManager.beginDelayedTransition(binding.headerRoot as ViewGroup)
        binding.cvSearchBar.visibility = View.GONE
        binding.tvCategoryTitle.visibility = View.VISIBLE
        binding.btnSearch.visibility = View.VISIBLE
        binding.etSearch.text.clear()
        filterEvents("")
    }

    private fun sortEventsByDate() {
        val sortedList = if (isDateAscending) {
            filteredEvents.sortedBy { it.eventDate }
        } else {
            filteredEvents.sortedByDescending { it.eventDate }
        }
        filteredEvents.clear()
        filteredEvents.addAll(sortedList)
        adapter.submitList(filteredEvents.toList())
        
        binding.tvSortLabel.text = if (isDateAscending) "Date: Nearest" else "Date: Latest"
        binding.btnSortDate.rotation = if (isDateAscending) 0f else 180f
    }

    private fun sortEventsByPrice() {
        val sortedList = if (isPriceAscending) {
            filteredEvents.sortedBy { it.eventPrice }
        } else {
            filteredEvents.sortedByDescending { it.eventPrice }
        }
        filteredEvents.clear()
        filteredEvents.addAll(sortedList)
        adapter.submitList(filteredEvents.toList())
        
        binding.btnSortPrice.rotation = if (isPriceAscending) 0f else 180f
    }

    private fun filterEvents(query: String) {
        filteredEvents = if (query.isEmpty()) {
            allCategoryEvents.toMutableList()
        } else {
            allCategoryEvents.filter { it.eventName.contains(query, ignoreCase = true) }.toMutableList()
        }
        adapter.submitList(filteredEvents.toList())
        updateEmptyState(query.isNotEmpty())
    }

    private fun updateEmptyState(isSearching: Boolean) {
        if (filteredEvents.isEmpty()) {
            binding.rvCategoryEvents.visibility = View.GONE
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.tvEmptyState.text = if (isSearching) "event tidak ditemukan" else "Belum ada event terdaftar"
        } else {
            binding.rvCategoryEvents.visibility = View.VISIBLE
            binding.tvEmptyState.visibility = View.GONE
        }
    }

    private fun loadEvents(category: String) {
        SupabaseRepository.loadApprovedEvents { result ->
            result.onSuccess { events ->
                allCategoryEvents = when (category) {
                    "All" -> events.toMutableList()
                    "Lainnya" -> {
                        val knownCategories = listOf("Seminar", "Workshop", "Dies Natalis")
                        events.filter { it.category !in knownCategories }.toMutableList()
                    }
                    else -> events.filter { it.category.equals(category, ignoreCase = true) }.toMutableList()
                }
                filteredEvents = allCategoryEvents.toMutableList()
                sortEventsByDate() // Default sort
            }
        }
    }
}
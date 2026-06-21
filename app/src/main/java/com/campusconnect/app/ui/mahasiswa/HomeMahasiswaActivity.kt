package com.campusconnect.app.ui.mahasiswa

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.campusconnect.app.R
import com.campusconnect.app.adapter.EventMahasiswaAdapter
import com.campusconnect.app.databinding.ActivityHomeMahasiswaBinding
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import com.campusconnect.app.ui.profile.ProfileActivity
import com.campusconnect.app.utils.setBlinkOnClick

class HomeMahasiswaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeMahasiswaBinding
    private lateinit var adapter: EventMahasiswaAdapter
    private lateinit var headerAdapter: EventMahasiswaAdapter

    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        val totalItems = binding.vpEventSlider.adapter?.itemCount ?: 0
        if (totalItems > 0) {
            val nextItem = binding.vpEventSlider.currentItem + 1
            binding.vpEventSlider.setCurrentItem(nextItem, true)
        }
    }

    private val allEvents = mutableListOf<Event>()
    private val filteredEvents = mutableListOf<Event>()
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeMahasiswaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupListeners()
        loadWelcomeName()
        loadEvents()
        setupBottomNavigation()
    }

    private fun setupPageTransformer(viewPager: ViewPager2) {
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(40))
        transformer.addTransformer { page, position ->
            val r = 1 - Math.abs(position)
            page.scaleY = 0.85f + r * 0.15f
            page.alpha = 0.5f + r * 0.5f
        }
        viewPager.setPageTransformer(transformer)
    }

    private fun setupRecyclerViews() {
        headerAdapter = EventMahasiswaAdapter(filteredEvents, useHeaderImage = true) { event ->
            openEventDetail(event)
        }

        adapter = EventMahasiswaAdapter(filteredEvents) { event ->
            openEventDetail(event)
        }

        binding.rvEvents.adapter = adapter
        binding.rvEvents.offscreenPageLimit = 3
        setupPageTransformer(binding.rvEvents)

        binding.vpEventSlider.adapter = headerAdapter
        binding.vpEventSlider.offscreenPageLimit = 3
        setupPageTransformer(binding.vpEventSlider)

        binding.vpEventSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })
    }

    private fun openEventDetail(event: Event) {
            val intent = Intent(this, DetailEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            intent.putExtra("eventPrice", event.eventPrice)
            startActivity(intent)
    }

    private fun setupListeners() {
        binding.ivBell.setBlinkOnClick {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.etSearchEvent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCatSeminar.setBlinkOnClick { navigateToCategory("Seminar") }
        binding.btnCatWorkshop.setBlinkOnClick { navigateToCategory("Workshop") }
        binding.btnCatDiesNatalis.setBlinkOnClick { navigateToCategory("Dies Natalis") }
        binding.btnCatLainnya.setBlinkOnClick { navigateToCategory("Lainnya") }

        binding.tvSeeAll.setBlinkOnClick {
            navigateToCategory("All")
        }
    }

    private fun navigateToCategory(category: String) {
        val intent = Intent(this, CategoryEventsActivity::class.java)
        intent.putExtra("category", category)
        startActivity(intent)
    }

    private fun applyFilters() {
        val results = allEvents.filter { it.eventName.contains(currentSearchQuery, ignoreCase = true) }
        filteredEvents.clear()
        filteredEvents.addAll(results)
        adapter.notifyDataSetChanged()
        headerAdapter.notifyDataSetChanged()
    }

    private fun loadEvents() {
        SupabaseRepository.loadApprovedEvents { result ->
            result.onSuccess { events ->
                allEvents.clear()
                allEvents.addAll(events)
                applyFilters()

                if (events.isNotEmpty()) {
                    val midPos = Int.MAX_VALUE / 2
                    val startPos = midPos - (midPos % events.size)

                    binding.rvEvents.setCurrentItem(startPos, false)
                    binding.vpEventSlider.setCurrentItem(startPos, false)

                    sliderHandler.postDelayed(sliderRunnable, 3000)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (allEvents.isNotEmpty()) {
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_ticket -> {
                    startActivity(Intent(this, ManageTicketActivity::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadWelcomeName() {
        val user = SupabaseRepository.currentUser(this)
        user?.let {
            SupabaseRepository.loadUserProfile(this, it.uid) { }
        }
    }
}

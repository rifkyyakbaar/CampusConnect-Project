package com.campusconnect.app.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event

class DashboardAdminActivity : AppCompatActivity() {
    private lateinit var rvAdminApprovals: RecyclerView
    private val eventList = mutableListOf<Event>()
    private lateinit var adapter: EventAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        rvAdminApprovals = findViewById(R.id.rvAdminApprovals)
        adapter = EventAdminAdapter(eventList) { event ->
            val intent = Intent(this, VerifyEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }

        rvAdminApprovals.layoutManager = LinearLayoutManager(this)
        rvAdminApprovals.adapter = adapter

        loadEvents()
    }

    private fun loadEvents() {
        SupabaseRepository.loadPendingEvents { result ->
            result.onSuccess { events ->
                eventList.clear()
                eventList.addAll(events)
                adapter.notifyDataSetChanged()
            }
        }
    }
}

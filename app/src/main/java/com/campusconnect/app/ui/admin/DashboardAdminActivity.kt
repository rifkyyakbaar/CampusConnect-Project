package com.campusconnect.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event

class DashboardAdminActivity : AppCompatActivity() {

    private lateinit var rvAdminApprovals: RecyclerView
    private lateinit var tvPendingApprovalMessage: TextView
    private val eventList = mutableListOf<Event>()
    private lateinit var adapter: EventAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        rvAdminApprovals = findViewById(R.id.rvAdminApprovals)
        tvPendingApprovalMessage = findViewById(R.id.tvPendingApprovalMessage)

        adapter = EventAdminAdapter(eventList) { event ->
            val intent = Intent(this, VerifyEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }

        rvAdminApprovals.layoutManager = LinearLayoutManager(this)
        rvAdminApprovals.adapter = adapter

        loadEvents()
    }

    override fun onResume() {
        super.onResume()
        loadEvents()
    }

    private fun loadEvents() {
        SupabaseRepository.loadAdminEvents { result ->
            result.onSuccess { events ->
                eventList.clear()
                eventList.addAll(events)
                adapter.notifyDataSetChanged()
                updatePendingMessage(events.count { it.status.equals("pending", ignoreCase = true) })
            }
        }
    }

    private fun updatePendingMessage(pendingCount: Int) {
        tvPendingApprovalMessage.text = when (pendingCount) {
            0 -> "No events waiting for approval."
            1 -> "You have 1 event waiting for approval."
            else -> "You have $pendingCount events waiting for approval."
        }
    }
}

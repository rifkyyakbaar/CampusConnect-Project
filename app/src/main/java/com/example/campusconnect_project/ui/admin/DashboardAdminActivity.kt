package com.example.campusconnect_project.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.campusconnect_project.R
import android.content.Intent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campusconnect_project.model.Event
import com.google.firebase.firestore.FirebaseFirestore


class DashboardAdminActivity : AppCompatActivity() {
    private lateinit var rvAdminApprovals: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val eventList = mutableListOf<Event>()
    private lateinit var adapter: EventAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        rvAdminApprovals =
            findViewById(R.id.rvAdminApprovals)

        adapter = EventAdminAdapter(eventList) { event ->

            val intent =
                Intent(
                    this,
                    VerifyEventActivity::class.java
                )

            intent.putExtra(
                "eventId",
                event.id
            )

            startActivity(intent)
        }

        rvAdminApprovals.layoutManager =
            LinearLayoutManager(this)

        rvAdminApprovals.adapter =
            adapter

        loadEvents()
    }

    private fun loadEvents() {

        firestore.collection("events")
            .whereEqualTo(
                "status",
                "pending"
            )
            .get()
            .addOnSuccessListener { result ->

                eventList.clear()

                for (document in result) {

                    val event =
                        document.toObject(Event::class.java)

                    event.id = document.id

                    eventList.add(event)
                }

                adapter.notifyDataSetChanged()
            }
    }
}
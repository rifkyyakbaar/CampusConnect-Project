package com.campusconnect.app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.model.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventPanitiaAdapter(
    private val eventList: List<Event>,
    private val onDetailClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit
) : RecyclerView.Adapter<EventPanitiaAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)
        val tvRegistrants: TextView = itemView.findViewById(R.id.tvRegistrants)
        val btnViewDetail: Button = itemView.findViewById(R.id.btnViewDetail)
        val btnEditEvent: Button = itemView.findViewById(R.id.btnEditEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_panitia, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        val rawStatus = event.status.ifBlank { "pending" }

        // Status efektif yang ditampilkan di badge card: kalau event
        // sudah "approved" tapi waktunya sudah lewat, badge tampil
        // "FINISHED" (abu-abu) — supaya selalu konsisten dengan kategori
        // filter navbar di atas, bukan hanya membaca status mentah dari
        // database (yang memang tidak pernah menyimpan nilai "finished").
        val displayStatus = if (
            rawStatus.equals("approved", ignoreCase = true) && isEventFinished(event)
        ) {
            "finished"
        } else {
            rawStatus
        }

        holder.tvEventName.text = event.eventName.ifBlank { "Untitled Event" }
        holder.tvCategory.text = event.category.ifBlank { "-" }
        holder.tvStatus.text = displayStatus.uppercase()
        holder.tvStatus.setTextColor(statusColor(displayStatus))
        holder.tvCapacity.text = "Capacity : ${event.capacity}"
        holder.tvRegistrants.text = "Registrants : ${event.registrants}"
        holder.btnViewDetail.setOnClickListener {
            onDetailClick(event)
        }
        holder.btnEditEvent.setOnClickListener {
            onEditClick(event)
        }
    }

    override fun getItemCount(): Int = eventList.size

    private fun statusColor(status: String): Int {
        return when {
            status.equals("approved", ignoreCase = true) -> Color.rgb(22, 163, 74)
            status.equals("rejected", ignoreCase = true) -> Color.rgb(220, 38, 38)
            status.equals("finished", ignoreCase = true) -> Color.rgb(176, 176, 176)
            else -> Color.rgb(245, 158, 11)
        }
    }

    companion object {
        /**
         * Event dianggap selesai jika waktu sekarang sudah melewati
         * eventDate (format gabungan "yyyy-MM-dd HH:mm" dari
         * CreateEventActivity). Dipakai juga oleh DashboardPanitiaActivity
         * untuk filter "Finished" — logic harus identik di kedua tempat.
         */
        fun isEventFinished(event: Event): Boolean {
            return runCatching {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val eventDateTime = format.parse(event.eventDate) ?: return false
                eventDateTime.before(Date())
            }.getOrDefault(false)
        }
    }
}
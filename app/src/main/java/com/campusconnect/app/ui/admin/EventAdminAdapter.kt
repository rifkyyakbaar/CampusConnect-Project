package com.campusconnect.app.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.Glide
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.model.Event

class EventAdminAdapter(
    private val eventList: List<Event>,
    private val onReviewClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdminAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val imgPoster: ImageView =
            itemView.findViewById(R.id.imgPoster)

        val tvEventName: TextView =
            itemView.findViewById(R.id.tvEventName)

        val tvOrganizer: TextView =
            itemView.findViewById(R.id.tvOrganizer)

        val tvCategory: TextView =
            itemView.findViewById(R.id.tvCategory)

        val tvLocation: TextView =
            itemView.findViewById(R.id.tvLocation)

        val tvEventDate: TextView =
            itemView.findViewById(R.id.tvEventDate)

        val tvCapacity: TextView =
            itemView.findViewById(R.id.tvCapacity)

        val tvStatus: TextView =
            itemView.findViewById(R.id.tvStatus)

        val btnReview: Button =
            itemView.findViewById(R.id.btnReview)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_event_admin,
                parent,
                false
            )

        return EventViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: EventViewHolder,
        position: Int
    ) {

        val event = eventList[position]
        val status = event.status.ifBlank { "pending" }

        holder.tvEventName.text =
            event.eventName

        holder.tvOrganizer.text =
            "By: ${event.organizerName}"

        holder.tvCategory.text =
            event.category

        holder.tvLocation.text =
            event.location

        holder.tvEventDate.text =
            "Start : ${event.eventDate.ifBlank { "-" }}"

        holder.tvCapacity.text =
            "Capacity : ${event.capacity}"

        holder.tvStatus.text =
            status.uppercase()
        holder.tvStatus.setTextColor(statusColor(status))
        holder.btnReview.text = if (status.equals("pending", ignoreCase = true)) {
            "Review"
        } else {
            "View"
        }

        if (event.posterUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(event.posterUrl)
                .placeholder(R.drawable.logo_campus_connect)
                .error(R.drawable.logo_campus_connect)
                .into(holder.imgPoster)
        }

        holder.btnReview.setOnClickListener {
            onReviewClick(event)
        }
    }
    override fun getItemCount(): Int {
        return eventList.size
    }

    private fun statusColor(status: String): Int {
        return when {
            status.equals("approved", ignoreCase = true) -> Color.rgb(22, 163, 74)
            status.equals("rejected", ignoreCase = true) -> Color.rgb(220, 38, 38)
            else -> Color.rgb(245, 158, 11)
        }
    }
}

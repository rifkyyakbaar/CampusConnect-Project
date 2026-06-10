package com.campusconnect.app.ui.admin

import android.view.LayoutInflater
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

        val tvEventName: TextView =
            itemView.findViewById(R.id.tvEventName)

        val tvOrganizer: TextView =
            itemView.findViewById(R.id.tvOrganizer)

        val tvCategory: TextView =
            itemView.findViewById(R.id.tvCategory)

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

        holder.tvEventName.text =
            event.eventName

        holder.tvOrganizer.text =
            "By: ${event.organizerName}"

        holder.tvCategory.text =
            event.category

        holder.tvCapacity.text =
            "Capacity : ${event.capacity}"

        holder.tvStatus.text =
            event.status.uppercase()

        holder.btnReview.setOnClickListener {
            onReviewClick(event)
        }
    }

    override fun getItemCount(): Int {
        return eventList.size
    }
}
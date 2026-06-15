package com.campusconnect.app.ui.mahasiswa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.model.Event

class EventMahasiswaAdapter(
    private val eventList: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventMahasiswaAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)

        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvOrganizer: TextView = itemView.findViewById(R.id.tvOrganizer)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvCapacity: TextView = itemView.findViewById(R.id.tvCapacity)

        val btnDetail: Button = itemView.findViewById(R.id.btnDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_mahasiswa, parent, false)

        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {

        val event = eventList[position]

        holder.tvEventName.text = event.eventName
        holder.tvOrganizer.text = "By : ${event.organizerName}"
        holder.tvCategory.text = event.category
        holder.tvLocation.text = event.location
        holder.tvCapacity.text = "Capacity : ${event.capacity}"

        Glide.with(holder.itemView.context)
            .load(event.posterUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgPoster)

        holder.btnDetail.setOnClickListener {
            onClick(event)
        }
    }

    override fun getItemCount(): Int {
        return eventList.size
    }
}
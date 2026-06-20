package com.campusconnect.app.ui.mahasiswa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.model.Event
import com.campusconnect.app.utils.setBlinkOnClick

class EventMahasiswaAdapter(
    private val eventList: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventMahasiswaAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgPoster: ImageView = itemView.findViewById(R.id.imgPoster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_mahasiswa, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        if (eventList.isEmpty()) return

        val event = eventList[position % eventList.size]

        Glide.with(holder.itemView.context)
            .load(event.posterUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgPoster)

        holder.itemView.setBlinkOnClick { onClick(event) }
    }

    override fun getItemCount(): Int = if (eventList.isNotEmpty()) Int.MAX_VALUE else 0
}
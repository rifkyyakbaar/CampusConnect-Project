package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.campusconnect.app.databinding.ItemEventGridBinding
import com.campusconnect.app.databinding.ItemEventListBinding
import com.campusconnect.app.model.Event
import com.campusconnect.app.utils.setBlinkOnClick
import java.text.NumberFormat
import java.util.Locale

class CategoryEventsAdapter(private val onItemClick: (Event) -> Unit) :
    ListAdapter<Event, CategoryEventsAdapter.EventViewHolder>(DiffCallback) {

    private var isGridView: Boolean = true

    fun setViewType(isGrid: Boolean) {
        this.isGridView = isGrid
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = if (viewType == VIEW_TYPE_GRID) {
            ItemEventGridBinding.inflate(layoutInflater, parent, false)
        } else {
            ItemEventListBinding.inflate(layoutInflater, parent, false)
        }
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(event: Event) {
            when (binding) {
                is ItemEventGridBinding -> {
                    binding.tvEventName.text = event.eventName
                    binding.tvEventDate.text = event.eventDate
                    binding.tvEventDetail.text = event.description
                    binding.tvEventPrice.text = formatPrice(event.eventPrice)
                    Glide.with(binding.ivEventPoster.context)
                        .load(event.posterUrl)
                        .into(binding.ivEventPoster)
                    binding.root.setBlinkOnClick { onItemClick(event) }
                }
                is ItemEventListBinding -> {
                    binding.tvEventName.text = event.eventName
                    binding.tvEventDate.text = event.eventDate
                    binding.tvEventPrice.text = formatPrice(event.eventPrice)
                    Glide.with(binding.ivEventPoster.context)
                        .load(event.posterUrl)
                        .into(binding.ivEventPoster)
                    binding.root.setBlinkOnClick { onItemClick(event) }
                }
            }
        }

        private fun formatPrice(price: Int): String {
            return if (price == 0) "Free"
            else NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(price)
        }
    }

    companion object {
        private const val VIEW_TYPE_GRID = 1
        private const val VIEW_TYPE_LIST = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean = oldItem == newItem
        }
    }
}
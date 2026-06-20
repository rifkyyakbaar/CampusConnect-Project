package com.campusconnect.app.ui.mahasiswa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.databinding.ItemTicketMahasiswaBinding
import com.campusconnect.app.model.Ticket
import com.campusconnect.app.utils.setBlinkOnClick

class TicketAdapter(
    private val onClick: (Ticket) -> Unit
) : ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(DiffCallback) {

    inner class TicketViewHolder(
        private val binding: ItemTicketMahasiswaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.tvCategory.text = ticket.category.uppercase()
            binding.tvEventName.text = ticket.eventName
            binding.tvDateVenue.text = "${ticket.eventDate} • ${ticket.eventLocation}"
            binding.tvTicketId.text = ticket.ticketId
            binding.tvStatus.text = ticket.status

            binding.btnViewTicket.setBlinkOnClick {
                onClick(ticket)
            }

            binding.root.setBlinkOnClick {
                onClick(ticket)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketMahasiswaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Ticket>() {
            override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
                return oldItem.ticketId == newItem.ticketId
            }

            override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean {
                return oldItem == newItem
            }
        }
    }
}
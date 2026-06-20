package com.campusconnect.app.ui.mahasiswa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.databinding.ItemTicketMahasiswaBinding
import com.campusconnect.app.model.Ticket

class TicketAdapter(
    private val tickets: List<Ticket>,
    private val onClick: (Ticket) -> Unit
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    inner class TicketViewHolder(
        private val binding: ItemTicketMahasiswaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {

            binding.tvCategory.text = ticket.category.uppercase()

            binding.tvEventName.text = ticket.eventName

            binding.tvDateVenue.text =
                "${ticket.eventDate} • ${ticket.eventLocation}"

            binding.tvTicketId.text = ticket.ticketId

            binding.tvStatus.text = ticket.status

            binding.btnViewTicket.setOnClickListener {
                onClick(ticket)
            }

            binding.root.setOnClickListener {
                onClick(ticket)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TicketViewHolder {

        val binding = ItemTicketMahasiswaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: TicketViewHolder,
        position: Int
    ) {
        holder.bind(tickets[position])
    }

    override fun getItemCount(): Int {
        return tickets.size
    }
}
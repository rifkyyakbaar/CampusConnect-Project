package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.databinding.ItemTicketMahasiswaBinding
import com.campusconnect.app.model.Ticket
import com.campusconnect.app.utils.setBlinkOnClick

class TicketAdapter(
    private val onClick: (Ticket) -> Unit,
    private val onReview: ((Ticket) -> Unit)? = null   // null = tidak tampilkan tombol review
) : ListAdapter<Ticket, TicketAdapter.TicketViewHolder>(DiffCallback) {

    inner class TicketViewHolder(
        private val binding: ItemTicketMahasiswaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            binding.tvCategory.text  = ticket.category.uppercase()
            binding.tvEventName.text = ticket.eventName
            binding.tvDateVenue.text = "${ticket.eventDate} • ${ticket.eventLocation}"
            binding.tvTicketId.text  = ticket.ticketId
            binding.tvStatus.text    = ticket.status

            // LOGIKA PENDING & CONFIRMED
            if (ticket.status.equals("PENDING", ignoreCase = true)) {
                // Sembunyikan tombol View Ticket
                binding.btnViewTicket.visibility = android.view.View.GONE

                // Ubah warna teks dan titik status menjadi Abu-abu/Kuning
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#FFA500")) // Warna Orange untuk Pending
                // (Opsional) Jika Anda punya akses ke View bullet point-nya, Anda bisa ubah warnanya juga di sini

                // Matikan klik pada seluruh card agar tidak bisa membuka TicketActivity
                binding.root.setOnClickListener(null)
            } else {
                // Munculkan kembali tombol View Ticket
                binding.btnViewTicket.visibility = android.view.View.VISIBLE

                // Kembalikan warna teks status ke warna default (misalnya Hijau untuk Confirmed)
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

                // Aktifkan kembali fungsi klik
                binding.btnViewTicket.setBlinkOnClick { onClick(ticket) }
                binding.root.setBlinkOnClick { onClick(ticket) }
            }

            // Tombol Review — hanya muncul di History (onReview != null) & status USED
            if (onReview != null && ticket.status == "USED") {
                binding.btnReview.visibility = android.view.View.VISIBLE
                binding.btnReview.setOnClickListener { onReview.invoke(ticket) }
            } else {
                binding.btnReview.visibility = android.view.View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketMahasiswaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Ticket>() {
            override fun areItemsTheSame(old: Ticket, new: Ticket) = old.ticketId == new.ticketId
            override fun areContentsTheSame(old: Ticket, new: Ticket) = old == new
        }
    }
}

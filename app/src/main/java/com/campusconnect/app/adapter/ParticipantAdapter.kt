package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.model.Peserta

class ParticipantAdapter(
    private val participants: List<Peserta>,
    private val onApprove:   (Peserta) -> Unit,
    private val onReject:    (Peserta) -> Unit,
    private val onViewProof: (Peserta) -> Unit,
    private val onReview:    (Peserta) -> Unit
) : RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName:           TextView     = itemView.findViewById(R.id.tvParticipantName)
        val tvRole:           TextView     = itemView.findViewById(R.id.tvParticipantRole)
        val tvStatus:         TextView     = itemView.findViewById(R.id.tvParticipantStatus)
        val btnReview:        Button       = itemView.findViewById(R.id.btnReview)
        val layoutActions:    LinearLayout = itemView.findViewById(R.id.layoutActionButtons)
        val btnViewProof:     Button       = itemView.findViewById(R.id.btnViewProof)
        val btnApprove:       Button       = itemView.findViewById(R.id.btnApprove)
        val btnReject:        Button       = itemView.findViewById(R.id.btnReject)
        val spaceApprove:     Space        = itemView.findViewById(R.id.spaceApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = participants.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = participants[position]

        holder.tvName.text   = p.attendeeName
        holder.tvRole.text   = p.attendeeRole
        holder.tvStatus.text = p.status

        val isPaid = p.paymentProofUrl.isNotBlank()

        // ── Tombol Lihat Review (pojok kanan atas) ────────────────
        // Hanya muncul jika tiket sudah USED
        if (p.status == "USED") {
            holder.btnReview.visibility = View.VISIBLE
            holder.btnReview.setOnClickListener { onReview(p) }
        } else {
            holder.btnReview.visibility = View.GONE
        }

        // ── Tombol aksi bawah ────────────────────────────────────
        // Hanya muncul untuk event BERBAYAR (paymentProofUrl tidak kosong)
        if (isPaid) {
            holder.layoutActions.visibility = View.VISIBLE
            holder.btnViewProof.setOnClickListener { onViewProof(p) }

            when (p.status) {
                "PENDING" -> {
                    // Tampilkan Approve + Reject
                    holder.btnApprove.visibility  = View.VISIBLE
                    holder.btnReject.visibility   = View.VISIBLE
                    holder.spaceApprove.visibility = View.VISIBLE
                    holder.btnApprove.setOnClickListener { onApprove(p) }
                    holder.btnReject.setOnClickListener  { onReject(p) }
                }
                "CONFIRMED", "USED" -> {
                    // Sudah diproses — hanya Lihat Bukti saja
                    holder.btnApprove.visibility  = View.GONE
                    holder.btnReject.visibility   = View.GONE
                    holder.spaceApprove.visibility = View.GONE
                }
                else -> {
                    // REJECTED atau status lain — sembunyikan semua aksi
                    holder.layoutActions.visibility = View.GONE
                }
            }
        } else {
            // Event GRATIS — tidak ada tombol aksi bawah sama sekali
            holder.layoutActions.visibility = View.GONE
        }
    }
}
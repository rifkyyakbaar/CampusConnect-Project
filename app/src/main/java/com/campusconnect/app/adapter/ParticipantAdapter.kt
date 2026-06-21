package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.model.Peserta

class ParticipantAdapter(
    private val participants: List<Peserta>,
    private val onApprove: (Peserta) -> Unit,
    private val onReject: (Peserta) -> Unit,
    private val onViewProof: (Peserta) -> Unit
) : RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvName: TextView =
            itemView.findViewById(R.id.tvParticipantName)

        val tvRole: TextView =
            itemView.findViewById(R.id.tvParticipantRole)

        val tvStatus: TextView =
            itemView.findViewById(R.id.tvParticipantStatus)

        val btnApprove: Button =
            itemView.findViewById(R.id.btnApprove)

        val btnReject: Button =
            itemView.findViewById(R.id.btnReject)

        val btnViewProof: Button =
            itemView.findViewById(R.id.btnViewProof)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_participant,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val participant = participants[position]

        holder.tvName.text =
            participant.attendeeName

        holder.tvRole.text =
            participant.attendeeRole

        holder.tvStatus.text =
            participant.status

        holder.btnViewProof.setOnClickListener {

            onViewProof(participant)

        }

        holder.btnApprove.setOnClickListener {

            onApprove(participant)

        }

        holder.btnReject.setOnClickListener {

            onReject(participant)

        }

        // kalau sudah CONFIRMED atau REJECTED,
        // tombol approve reject disembunyikan

        if (
            participant.status == "CONFIRMED" ||
            participant.status == "REJECTED"
        ) {

            holder.btnApprove.visibility = View.GONE
            holder.btnReject.visibility = View.GONE

        } else {

            holder.btnApprove.visibility = View.VISIBLE
            holder.btnReject.visibility = View.VISIBLE
        }
    }
}
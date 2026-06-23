package com.campusconnect.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.model.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val items = mutableListOf<Notification>()

    fun submitList(list: List<Notification>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView    = view.findViewById(R.id.tvNotifIcon)
        val tvTitle: TextView   = view.findViewById(R.id.tvNotifTitle)
        val tvMessage: TextView = view.findViewById(R.id.tvNotifMessage)
        val tvTime: TextView    = view.findViewById(R.id.tvNotifTime)
        val dotUnread: View     = view.findViewById(R.id.dotUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = items[position]

        holder.tvTitle.text   = notif.title
        holder.tvMessage.text = notif.message
        holder.tvTime.text    = formatRelativeTime(notif.createdAt)
        holder.tvIcon.text    = iconForType(notif.type)
        holder.dotUnread.visibility = if (notif.isRead) View.GONE else View.VISIBLE

        val bgColor = if (notif.isRead) 0xFFFFFFFF.toInt() else 0xFFF0F7FF.toInt()
        (holder.itemView as? androidx.cardview.widget.CardView)?.setCardBackgroundColor(bgColor)

        holder.itemView.setOnClickListener { onItemClick(notif) }
    }

    private fun iconForType(type: String): String = when (type) {
        "PAYMENT_APPROVED"  -> "✅"
        "PAYMENT_REJECTED"  -> "❌"
        "EVENT_APPROVED"    -> "🎉"
        "EVENT_REJECTED"    -> "😔"
        "EVENT_UPDATED"     -> "📋"
        "EVENT_REMINDER_H1" -> "📅"
        "EVENT_REMINDER_1H" -> "⏰"
        "EVENT_REMINDER_10M"-> "🚀"
        else                -> "🔔"
    }

    private fun formatRelativeTime(createdAt: String): String {
        return runCatching {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.US),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            )
            val date: Date = formats.firstNotNullOfOrNull { fmt ->
                runCatching { fmt.parse(createdAt) }.getOrNull()
            } ?: return "Baru saja"

            val diff  = System.currentTimeMillis() - date.time
            val mins  = diff / 60_000
            val hours = diff / 3_600_000
            val days  = diff / 86_400_000

            when {
                mins  < 1  -> "Baru saja"
                mins  < 60 -> "$mins menit yang lalu"
                hours < 24 -> "$hours jam yang lalu"
                days  < 7  -> "$days hari yang lalu"
                else       -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(date)
            }
        }.getOrDefault("Baru saja")
    }
}
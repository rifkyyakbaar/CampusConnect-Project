package com.campusconnect.app.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.User

class UserAdapter(
    private val userList: List<User>,
    private val context: Context,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgUserAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = itemView.findViewById(R.id.tvUserRole)
        val tvStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        holder.tvName.text = user.fullName.ifBlank { "Pengguna" }
        holder.tvEmail.text = user.email
        holder.tvRole.text = user.role

        // Badge warna sesuai accountStatus
        val (badgeBg, badgeText) = when (user.accountStatus.uppercase()) {
            "BANNED"  -> Color.parseColor("#FEF3C7") to Color.parseColor("#92400E")  // amber
            "DELETED" -> Color.parseColor("#3A3A3A")  to Color.parseColor("#B0B0B0") // abu-abu
            else      -> Color.parseColor("#10B981")  to Color.WHITE                 // hijau ACTIVE
        }
        holder.tvStatus.setBackgroundColor(badgeBg)
        holder.tvStatus.setTextColor(badgeText)
        holder.tvStatus.text = user.accountStatus.uppercase()

        // Foto profil via Glide
        val avatarUrl = SupabaseRepository.getAvatarUrl(context, user.uid)
        Glide.with(context)
            .load(avatarUrl)
            .placeholder(android.R.drawable.ic_menu_myplaces)
            .error(android.R.drawable.ic_menu_myplaces)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    override fun getItemCount(): Int = userList.size
}
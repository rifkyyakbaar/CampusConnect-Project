package com.campusconnect.app.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.campusconnect.app.R
import com.campusconnect.app.model.User
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * Bottom sheet untuk aksi admin terhadap user: Ban, Unban, atau Hapus.
 * Tombol yang tampil disesuaikan otomatis berdasarkan accountStatus user:
 *  - ACTIVE  → Ban + Hapus
 *  - BANNED  → Unban + Hapus
 *  - DELETED → hanya info, tidak ada aksi
 */
class UserActionDialog : BottomSheetDialogFragment() {

    private var user: User? = null
    private var onBan: (() -> Unit)? = null
    private var onUnban: (() -> Unit)? = null
    private var onDelete: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.layout_dialog_user_action, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = user ?: return

        view.findViewById<TextView>(R.id.tvSheetUserName).text =
            currentUser.fullName.ifBlank { "Pengguna" }
        view.findViewById<TextView>(R.id.tvSheetUserEmail).text = currentUser.email

        val tvDeletedInfo = view.findViewById<TextView>(R.id.tvSheetDeletedInfo)
        val btnBan        = view.findViewById<MaterialButton>(R.id.btnBanUser)
        val btnUnban      = view.findViewById<MaterialButton>(R.id.btnUnbanUser)
        val btnDelete     = view.findViewById<MaterialButton>(R.id.btnDeleteUser)
        val btnCancel     = view.findViewById<MaterialButton>(R.id.btnCancelUserAction)

        when (currentUser.accountStatus.uppercase()) {
            "ACTIVE" -> {
                btnBan.visibility    = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }
            "BANNED" -> {
                btnUnban.visibility  = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }
            "DELETED" -> {
                tvDeletedInfo.visibility = View.VISIBLE
            }
        }

        btnBan.setOnClickListener {
            onBan?.invoke()
            dismiss()
        }
        btnUnban.setOnClickListener {
            onUnban?.invoke()
            dismiss()
        }
        btnDelete.setOnClickListener {
            onDelete?.invoke()
            dismiss()
        }
        btnCancel.setOnClickListener { dismiss() }
    }

    companion object {
        fun show(
            user: User,
            onBan: () -> Unit,
            onUnban: () -> Unit,
            onDelete: () -> Unit
        ): UserActionDialog {
            return UserActionDialog().apply {
                this.user     = user
                this.onBan    = onBan
                this.onUnban  = onUnban
                this.onDelete = onDelete
            }
        }
    }
}
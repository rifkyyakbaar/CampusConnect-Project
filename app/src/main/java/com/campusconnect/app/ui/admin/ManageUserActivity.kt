package com.campusconnect.app.ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.UserAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.User
import com.campusconnect.app.util.UserActionDialog
import com.google.android.material.bottomnavigation.BottomNavigationView

class ManageUserActivity : AppCompatActivity() {

    private lateinit var rvAdminUsers: RecyclerView
    private lateinit var etSearchUser: EditText

    // Pill filter role
    private lateinit var pillPeserta: CardView
    private lateinit var pillPanitia: CardView
    private lateinit var tvPillPeserta: TextView
    private lateinit var tvPillPanitia: TextView

    // Semua user dari server — jangan diubah kecuali saat reload
    private val allUsers = mutableListOf<User>()
    // User yang sedang ditampilkan setelah filter role + search
    private val displayList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    // "Mahasiswa" = Peserta, "Panitia" = Panitia
    private var selectedRole = "Mahasiswa"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_user)

        rvAdminUsers  = findViewById(R.id.rvAdminUsers)
        etSearchUser  = findViewById(R.id.etSearchUser)
        pillPeserta   = findViewById(R.id.pillPeserta)
        pillPanitia   = findViewById(R.id.pillPanitia)
        tvPillPeserta = findViewById(R.id.tvPillPeserta)
        tvPillPanitia = findViewById(R.id.tvPillPanitia)

        // Adapter menunjuk ke displayList
        adapter = UserAdapter(displayList, this) { user ->
            showUserActionDialog(user)
        }
        rvAdminUsers.layoutManager = LinearLayoutManager(this)
        rvAdminUsers.adapter = adapter

        setupPillListeners()
        setupSearch()
        setupBottomNavigation()
        setupBackButton()

        loadUsers()
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    // ── Setup ─────────────────────────────────────────────────────────────

    private fun setupPillListeners() {
        pillPeserta.setOnClickListener {
            selectedRole = "Mahasiswa"
            applyFilter()
        }
        pillPanitia.setOnClickListener {
            selectedRole = "Panitia"
            applyFilter()
        }
    }

    private fun setupSearch() {
        etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupBackButton() {
        findViewById<ImageView>(R.id.btnBackManageUser).setOnClickListener {
            startActivity(Intent(this, DashboardAdminActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            overridePendingTransition(0, 0)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_manage_user
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_manage_user -> true
                R.id.nav_manage_event -> {
                    startActivity(Intent(this, DashboardAdminActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    // ── Load & Filter ─────────────────────────────────────────────────────

    private fun loadUsers() {
        SupabaseRepository.loadAllUsers(this) { result ->
            result.onSuccess { users ->
                allUsers.clear()
                allUsers.addAll(users)
                applyFilter()
            }
            result.onFailure { exception ->
                Toast.makeText(this, "Gagal memuat user: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilter() {
        val query = searchQuery.lowercase()
        displayList.clear()
        displayList.addAll(
            allUsers.filter { user ->
                user.role.equals(selectedRole, ignoreCase = true) &&
                        (query.isBlank() ||
                                user.fullName.lowercase().contains(query) ||
                                user.email.lowercase().contains(query))
            }
        )
        adapter.notifyDataSetChanged()
        updatePillStates()
    }

    private fun updatePillStates() {
        // Reset ke nonaktif
        pillPeserta.setCardBackgroundColor(Color.parseColor("#30FFFFFF"))
        tvPillPeserta.setTextColor(Color.WHITE)
        pillPanitia.setCardBackgroundColor(Color.parseColor("#30FFFFFF"))
        tvPillPanitia.setTextColor(Color.WHITE)

        // Aktifkan pill yang dipilih dengan warna primer teal
        when (selectedRole) {
            "Mahasiswa" -> {
                pillPeserta.setCardBackgroundColor(Color.parseColor("#00BFA5")) // warna_primer
                tvPillPeserta.setTextColor(Color.WHITE)
            }
            "Panitia" -> {
                pillPanitia.setCardBackgroundColor(Color.parseColor("#00BFA5"))
                tvPillPanitia.setTextColor(Color.WHITE)
            }
        }
    }

    // ── Dialog aksi user ─────────────────────────────────────────────────

    private fun showUserActionDialog(user: User) {
        UserActionDialog.show(
            user  = user,
            onBan = { performBan(user) },
            onUnban = { performUnban(user) },
            onDelete = { performDelete(user) }
        ).show(supportFragmentManager, "UserActionDialog")
    }

    private fun performBan(user: User) {
        SupabaseRepository.banUser(this, user.uid) { result ->
            result.onSuccess {
                Toast.makeText(this, "${user.fullName} telah di-ban.", Toast.LENGTH_SHORT).show()
                updateUserStatusLocally(user.uid, "BANNED")
            }
            result.onFailure { exception ->
                Toast.makeText(this, "Gagal ban: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performUnban(user: User) {
        SupabaseRepository.unbanUser(this, user.uid) { result ->
            result.onSuccess {
                Toast.makeText(this, "${user.fullName} telah di-unban.", Toast.LENGTH_SHORT).show()
                updateUserStatusLocally(user.uid, "ACTIVE")
            }
            result.onFailure { exception ->
                Toast.makeText(this, "Gagal unban: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performDelete(user: User) {
        SupabaseRepository.deleteUserByAdmin(this, user.uid) { result ->
            result.onSuccess {
                Toast.makeText(this, "Akun ${user.fullName} telah dihapus.", Toast.LENGTH_SHORT).show()
                updateUserStatusLocally(user.uid, "DELETED")
            }
            result.onFailure { exception ->
                Toast.makeText(this, "Gagal hapus: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Update status user di allUsers secara lokal (tidak re-fetch dari server)
     * supaya UI langsung berubah tanpa menunggu round-trip.
     */
    private fun updateUserStatusLocally(uid: String, newStatus: String) {
        val index = allUsers.indexOfFirst { it.uid == uid }
        if (index >= 0) {
            allUsers[index] = allUsers[index].copy(accountStatus = newStatus)
        }
        applyFilter()
    }
}
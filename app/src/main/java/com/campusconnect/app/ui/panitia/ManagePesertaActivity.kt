package com.campusconnect.app.ui.panitia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.campusconnect.app.R
import com.campusconnect.app.adapter.ParticipantAdapter
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Peserta

class ManagePesertaActivity : AppCompatActivity() {

    private lateinit var rvDaftarPeserta: RecyclerView
    private lateinit var tvTotalPeserta: TextView
    private lateinit var btnBack: ImageView
    private lateinit var etSearchPeserta: EditText

    private var eventId = ""
    private var allParticipants: List<Peserta> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_peserta)

        eventId = intent.getStringExtra("eventId").orEmpty()

        initViews()
        loadParticipants()
    }

    private fun initViews() {

        rvDaftarPeserta =
            findViewById(R.id.rvDaftarPeserta)

        tvTotalPeserta =
            findViewById(R.id.tvTotalPeserta)

        btnBack =
            findViewById(R.id.btnBackManage)

        etSearchPeserta =
            findViewById(R.id.etSearchPeserta)

        rvDaftarPeserta.layoutManager =
            LinearLayoutManager(this)

        btnBack.setOnClickListener {

            finish()

        }

        etSearchPeserta.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterParticipants(s?.toString().orEmpty())
            }
        })
    }

    private fun loadParticipants() {

        SupabaseRepository.loadParticipants(this, eventId) { result ->

            result.onSuccess { participants ->

                allParticipants = participants

                tvTotalPeserta.text =
                    "Total : ${participants.size} Peserta"

                showParticipants(participants)

            }.onFailure {

                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun filterParticipants(query: String) {

        val filtered = if (query.isBlank()) {
            allParticipants
        } else {
            allParticipants.filter {
                it.attendeeName.contains(query, ignoreCase = true) ||
                        it.ticketId.contains(query, ignoreCase = true)
            }
        }

        tvTotalPeserta.text =
            "Total : ${filtered.size} Peserta"

        showParticipants(filtered)
    }

    private fun showParticipants(
        participants: List<Peserta>
    ) {

        val adapter =
            ParticipantAdapter(

                participants,

                onApprove = { peserta ->

                    approveParticipant(peserta)

                },

                onReject = { peserta ->

                    rejectParticipant(peserta)

                },

                onViewProof = { peserta ->

                    viewProof(peserta)

                }

            )

        rvDaftarPeserta.adapter = adapter
    }

    private fun approveParticipant(
        peserta: Peserta
    ) {

        SupabaseRepository.approveParticipant(
            this,
            peserta.ticketId
        ) { result ->

            result.onSuccess {

                Toast.makeText(
                    this,
                    "Peserta berhasil disetujui",
                    Toast.LENGTH_SHORT
                ).show()

                loadParticipants()

            }.onFailure {

                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun rejectParticipant(
        peserta: Peserta
    ) {

        SupabaseRepository.rejectParticipant(
            this,
            peserta.ticketId
        ) { result ->

            result.onSuccess {

                Toast.makeText(
                    this,
                    "Peserta ditolak",
                    Toast.LENGTH_SHORT
                ).show()

                loadParticipants()

            }.onFailure {

                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun viewProof(
        peserta: Peserta
    ) {

        if (peserta.paymentProofUrl.isBlank()) {

            Toast.makeText(
                this,
                "Peserta ini tidak memiliki bukti pembayaran (event gratis)",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        runCatching {

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(peserta.paymentProofUrl)
            }

            startActivity(intent)

        }.onFailure {

            Toast.makeText(
                this,
                "Tidak bisa membuka gambar bukti pembayaran",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
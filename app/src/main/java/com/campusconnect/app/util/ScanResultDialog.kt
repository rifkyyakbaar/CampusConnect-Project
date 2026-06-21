package com.campusconnect.app.util

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.campusconnect.app.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

/**
 * Popup hasil scan QR tiket, dipakai ulang untuk empat kondisi berbeda:
 *
 * - [Mode.SUCCESS_PANITIA]  — panitia berhasil scan, tiket CONFIRMED -> USED
 * - [Mode.USED_PANITIA]     — panitia scan tiket yang statusnya sudah USED
 * - [Mode.INVALID_PANITIA]  — QR tidak sah untuk event ini (salah event,
 *                              tidak ditemukan, ditolak, belum diverifikasi)
 * - [Mode.SUCCESS_PESERTA]  — peserta melihat tiketnya sendiri baru saja
 *                              di-scan dan dipakai oleh panitia
 *
 * Hanya satu layout (`layout_dialog_scan_result.xml`) yang dipakai untuk
 * keempatnya — isi, ikon, warna, dan tombol berubah sesuai mode.
 */
class ScanResultDialog : BottomSheetDialogFragment() {

    enum class Mode {
        SUCCESS_PANITIA,
        USED_PANITIA,
        INVALID_PANITIA,
        SUCCESS_PESERTA
    }

    private var mode: Mode = Mode.SUCCESS_PANITIA
    private var title: String = ""
    private var message: String = ""
    private var attendeeName: String? = null
    private var attendeeRole: String? = null
    private var detailLabel: String? = null
    private var detailMessage: String? = null

    private var onScanLagi: (() -> Unit)? = null
    private var onSelesai: (() -> Unit)? = null
    private var onOk: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.layout_dialog_scan_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        val iconCircle = view.findViewById<FrameLayout>(R.id.iconCircle)
        val ivDialogIcon = view.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDialogMessage = view.findViewById<TextView>(R.id.tvDialogMessage)
        val layoutAttendeeInfo = view.findViewById<LinearLayout>(R.id.layoutAttendeeInfo)
        val tvAttendeeName = view.findViewById<TextView>(R.id.tvAttendeeName)
        val tvAttendeeRole = view.findViewById<TextView>(R.id.tvAttendeeRole)
        val layoutButtonsPanitia = view.findViewById<LinearLayout>(R.id.layoutButtonsPanitia)
        val btnSelesai = view.findViewById<MaterialButton>(R.id.btnSelesai)
        val btnScanLagi = view.findViewById<MaterialButton>(R.id.btnScanLagi)
        val btnOkPeserta = view.findViewById<MaterialButton>(R.id.btnOkPeserta)

        tvDialogTitle.text = title
        tvDialogMessage.text = message

        val context = requireContext()

        // Warna & ikon berbeda sesuai mode: hijau/teal untuk success,
        // oranye untuk "sudah digunakan", merah untuk "tidak valid".
        val (iconColor, iconRes) = when (mode) {
            Mode.SUCCESS_PANITIA, Mode.SUCCESS_PESERTA ->
                context.getColor(R.color.bg_terima) to R.drawable.ic_check_circle

            Mode.USED_PANITIA ->
                context.getColor(R.color.warna_warning) to R.drawable.ic_history_clock

            Mode.INVALID_PANITIA ->
                context.getColor(R.color.teks_tolak) to R.drawable.ic_invalid_close
        }

        (iconCircle.background as GradientDrawable).setColor(iconColor)
        ivDialogIcon.setImageResource(iconRes)

        // Kotak detail abu-abu di bawah deskripsi dipakai untuk dua hal
        // berbeda tergantung mode — tidak pernah ditumpangkan satu sama lain:
        //  - mode sukses/used panitia -> nama & role peserta
        //  - mode tidak valid         -> label singkat + alasan tidak valid
        when {
            mode == Mode.INVALID_PANITIA && !detailLabel.isNullOrBlank() -> {
                layoutAttendeeInfo.visibility = View.VISIBLE
                tvAttendeeName.text = detailLabel
                tvAttendeeRole.text = detailMessage.orEmpty()
            }

            !attendeeName.isNullOrBlank() -> {
                layoutAttendeeInfo.visibility = View.VISIBLE
                tvAttendeeName.text = attendeeName
                tvAttendeeRole.text = attendeeRole.orEmpty()
            }

            else -> {
                layoutAttendeeInfo.visibility = View.GONE
            }
        }

        // Tombol berbeda sesuai mode: panitia dapat "Selesai" + "Scan Lagi",
        // peserta cukup satu tombol "OK".
        when (mode) {
            Mode.SUCCESS_PANITIA, Mode.USED_PANITIA, Mode.INVALID_PANITIA -> {
                layoutButtonsPanitia.visibility = View.VISIBLE
                btnOkPeserta.visibility = View.GONE

                btnSelesai.setOnClickListener {
                    onSelesai?.invoke()
                    dismiss()
                }

                btnScanLagi.setOnClickListener {
                    onScanLagi?.invoke()
                    dismiss()
                }
            }

            Mode.SUCCESS_PESERTA -> {
                layoutButtonsPanitia.visibility = View.GONE
                btnOkPeserta.visibility = View.VISIBLE

                btnOkPeserta.setOnClickListener {
                    onOk?.invoke()
                    dismiss()
                }
            }
        }
    }

    companion object {

        /**
         * Panitia berhasil scan QR, status tiket CONFIRMED -> USED.
         */
        fun showSuccessPanitia(
            attendeeName: String,
            attendeeRole: String,
            onScanLagi: () -> Unit,
            onSelesai: () -> Unit
        ): ScanResultDialog {
            return ScanResultDialog().apply {
                mode = Mode.SUCCESS_PANITIA
                title = "Tiket Berhasil Digunakan"
                message = "Peserta berhasil melakukan check-in ke acara."
                this.attendeeName = attendeeName
                this.attendeeRole = attendeeRole
                this.onScanLagi = onScanLagi
                this.onSelesai = onSelesai
            }
        }

        /**
         * Panitia scan QR yang tiketnya sudah berstatus USED sebelumnya.
         */
        fun showUsedPanitia(
            attendeeName: String,
            attendeeRole: String,
            onScanLagi: () -> Unit,
            onSelesai: () -> Unit
        ): ScanResultDialog {
            return ScanResultDialog().apply {
                mode = Mode.USED_PANITIA
                title = "Tiket Sudah Digunakan"
                message = "$attendeeName sudah pernah check-in sebelumnya."
                this.attendeeName = attendeeName
                this.attendeeRole = attendeeRole
                this.onScanLagi = onScanLagi
                this.onSelesai = onSelesai
            }
        }

        /**
         * Panitia scan QR yang tidak valid untuk event yang sedang dibuka:
         * salah event, tiket tidak ditemukan, pembayaran ditolak/belum
         * diverifikasi, atau status lain yang tidak dikenali. Berbeda dari
         * [showUsedPanitia] — ini bukan soal "sudah dipakai", tapi soal
         * "tiket ini tidak sah dipakai di sini sama sekali".
         */
        fun showInvalidPanitia(
            title: String,
            reason: String,
            onScanLagi: () -> Unit,
            onSelesai: () -> Unit
        ): ScanResultDialog {
            return ScanResultDialog().apply {
                mode = Mode.INVALID_PANITIA
                this.title = "QR Tidak Valid"
                message = "Tiket ini tidak dapat digunakan untuk acara ini."
                detailLabel = title
                detailMessage = reason
                this.onScanLagi = onScanLagi
                this.onSelesai = onSelesai
            }
        }

        /**
         * Peserta melihat tiketnya sendiri baru saja dipakai (di-scan panitia).
         */
        fun showSuccessPeserta(
            onOk: () -> Unit
        ): ScanResultDialog {
            return ScanResultDialog().apply {
                mode = Mode.SUCCESS_PESERTA
                title = "Tiket Anda Telah Digunakan"
                message = "Selamat datang di acara! Check-in Anda telah berhasil dilakukan."
                this.onOk = onOk
            }
        }
    }
}
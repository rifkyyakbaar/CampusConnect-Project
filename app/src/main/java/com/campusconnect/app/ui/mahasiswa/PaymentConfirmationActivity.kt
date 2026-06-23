package com.campusconnect.app.ui.mahasiswa

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.util.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentConfirmationActivity : AppCompatActivity() {

    private lateinit var tvEventName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvPaymentInfo: TextView
    private lateinit var imgProof: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var btnConfirmPayment: Button
    private lateinit var btnBack: ImageView

    private var selectedImageUri: Uri? = null

    private var eventId = ""
    private var eventName = ""
    private var eventPrice = 0
    private var paymentInfo = ""
    private var eventDate = ""
    private var eventLocation = ""
    private var category = ""

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {
                selectedImageUri = uri
                imgProof.setImageURI(uri)
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_confirmation)

        initViews()
        getIntentData()
        setupUI()
        setupListeners()
    }

    private fun initViews() {

        tvEventName = findViewById(R.id.tvEventName)
        tvPrice = findViewById(R.id.tvPrice)
        tvPaymentInfo = findViewById(R.id.tvPaymentInfo)

        imgProof = findViewById(R.id.imgProof)

        btnChooseImage = findViewById(R.id.btnChooseImage)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)
        btnBack = findViewById(R.id.btnBack)
    }

    private fun getIntentData() {

        eventId = intent.getStringExtra("eventId").orEmpty()
        eventName = intent.getStringExtra("eventName").orEmpty()
        eventPrice = intent.getIntExtra("eventPrice", 0)
        paymentInfo = intent.getStringExtra("paymentInfo").orEmpty()
        eventDate = intent.getStringExtra("eventDate").orEmpty()
        eventLocation = intent.getStringExtra("eventLocation").orEmpty()
        category = intent.getStringExtra("category").orEmpty()
    }

    private fun setupUI() {

        tvEventName.text = eventName
        tvPrice.text = "Rp ${String.format("%,d", eventPrice).replace(",", ".")}"
        tvPaymentInfo.text = paymentInfo
    }

    private fun setupListeners() {

        btnBack.setOnClickListener {
            finish()
        }

        btnChooseImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnConfirmPayment.setOnClickListener {

            if (selectedImageUri == null) {

                Toast.makeText(
                    this,
                    "Upload bukti pembayaran terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            SupabaseRepository.uploadPaymentProofAndCreateTicket(
                context = this,
                imageUri = selectedImageUri!!,
                eventId = eventId,
                eventName = eventName,
                category = category,
                eventDate = eventDate,
                eventLocation = eventLocation
            ) { result ->

                result.onSuccess {

                    Toast.makeText(
                        this,
                        "Pembayaran berhasil dikirim, menunggu persetujuan panitia",
                        Toast.LENGTH_LONG
                    ).show()

                    // Jadwalkan reminder meski tiket masih PENDING —
                    // kalau panitia approve, reminder sudah terjadwal.
                    runCatching {
                        val user = SupabaseRepository.currentUser(this)
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            .parse(eventDate)?.time ?: 0L
                        if (user != null && ts > System.currentTimeMillis()) {
                            ReminderScheduler.scheduleAllReminders(
                                context            = this,
                                userId             = user.uid,
                                eventId            = eventId,
                                eventName          = eventName,
                                eventDateTimestamp = ts
                            )
                        }
                    }

                    finish()

                }.onFailure {

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
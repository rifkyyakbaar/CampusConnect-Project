package com.campusconnect.app.ui.panitia

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.campusconnect.app.R
import com.campusconnect.app.data.SupabaseRepository
import com.campusconnect.app.model.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateEventActivity : AppCompatActivity() {
    private data class ImageRequirement(
        val minWidth: Int,
        val minHeight: Int,
        val requiredLandscape: Boolean
    )

    private data class ImageSize(
        val width: Int,
        val height: Int
    )

    private val maxImageFileSizeBytes = 8L * 1024L * 1024L

    private val headerImageRequirement = ImageRequirement(
        minWidth = 800,
        minHeight = 360,
        requiredLandscape = true
    )

    private val generalImageRequirement = ImageRequirement(
        minWidth = 600,
        minHeight = 800,
        requiredLandscape = false
    )

    private val eventStartCalendar = Calendar.getInstance()
    private val categoryOptions = listOf("Seminar", "Workshop", "Dies Natalies", "Lainnya")
    private var selectedGeneralImageUri: Uri? = null
    private var selectedHeaderImageUri: Uri? = null
    private var selectedEventDate = ""
    private var selectedEventTime = ""
    private var isEditMode = false
    private var editingEventId = ""
    private var existingPosterUrl = ""
    private var existingHeaderImageUrl = ""

    private val pickHeaderImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val imageSize = getImageSize(uri)
        val validationMessage = validateImage(uri, imageSize, headerImageRequirement)
        if (validationMessage != null) {
            selectedHeaderImageUri = null
            showImageRequirementDialog(
                title = "Gambar header tidak sesuai",
                message = validationMessage
            )
            return@registerForActivityResult
        }
        selectedHeaderImageUri = uri
        showSelectedImage(uri, R.id.ivHeaderImagePreview, R.id.layoutUploadHeaderPlaceholder)
    }

    private val pickGeneralImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        val imageSize = getImageSize(uri)
        val validationMessage = validateImage(uri, imageSize, generalImageRequirement)
        if (validationMessage != null) {
            selectedGeneralImageUri = null
            showImageRequirementDialog(
                title = "Gambar umum tidak sesuai",
                message = validationMessage
            )
            return@registerForActivityResult
        }
        selectedGeneralImageUri = uri
        showSelectedImage(uri, R.id.ivGeneralImagePreview, R.id.layoutUploadGeneralPlaceholder)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        backdashboard()
        setupCategoryDropdown()
        setupEventStartPickers()
        setupPosterPicker()
        ensurePanitiaAccess()
        setupEditModeIfNeeded()

        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        btnSubmit.setOnClickListener {
            publishEvent()
        }
    }

    private fun backdashboard() {
        val btnBackCreate = findViewById<ImageView>(R.id.btnBackCreate)
        btnBackCreate.setOnClickListener {
            finish()
        }
    }

    private fun setupPosterPicker() {
        findViewById<CardView>(R.id.cvUploadHeaderImage).setOnClickListener {
            pickHeaderImageLauncher.launch("image/*")
        }
        findViewById<CardView>(R.id.cvUploadGeneralImage).setOnClickListener {
            pickGeneralImageLauncher.launch("image/*")
        }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        findViewById<Spinner>(R.id.spCategory).adapter = adapter
    }

    private fun setupEditModeIfNeeded() {
        isEditMode = intent.getStringExtra("mode").equals("edit", ignoreCase = true)
        editingEventId = intent.getStringExtra("eventId").orEmpty()
        if (!isEditMode || editingEventId.isBlank()) return

        findViewById<TextView>(R.id.tvCreateEventTitle).text = "Edit Event"
        findViewById<Button>(R.id.btnSubmitEvent).text = "Simpan Perubahan"

        SupabaseRepository.loadEventById(editingEventId) { result ->
            result
                .onSuccess { event -> populateEditForm(event) }
                .onFailure { exception ->
                    Toast.makeText(this, exception.localizedMessage ?: "Gagal memuat event.", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun populateEditForm(event: Event) {
        existingPosterUrl = event.posterUrl
        existingHeaderImageUrl = event.headerImageUrl

        findViewById<EditText>(R.id.etEventName).setText(event.eventName)
        findViewById<EditText>(R.id.etLocation).setText(event.location)
        findViewById<EditText>(R.id.etCapacity).setText(event.capacity.toString())
        findViewById<EditText>(R.id.etDescription).setText(event.description)
        findViewById<EditText>(R.id.etTicketPrice).setText(if (event.eventPrice > 0) event.eventPrice.toString() else "")
        findViewById<EditText>(R.id.etPaymentInfo).setText(event.paymentInfo)

        val categoryIndex = categoryOptions.indexOfFirst { it.equals(event.category, ignoreCase = true) }
        if (categoryIndex >= 0) findViewById<Spinner>(R.id.spCategory).setSelection(categoryIndex)

        val parts = event.eventDate.split(" ", limit = 2)
        selectedEventDate = parts.getOrNull(0).orEmpty()
        selectedEventTime = parts.getOrNull(1).orEmpty()
        findViewById<EditText>(R.id.etEventDate).setText(selectedEventDate)
        findViewById<EditText>(R.id.etEventTime).setText(selectedEventTime)

        showExistingImage(event.headerImageUrl, R.id.ivHeaderImagePreview, R.id.layoutUploadHeaderPlaceholder)
        showExistingImage(event.posterUrl, R.id.ivGeneralImagePreview, R.id.layoutUploadGeneralPlaceholder)
    }

    private fun setupEventStartPickers() {
        val etEventDate = findViewById<EditText>(R.id.etEventDate)
        val etEventTime = findViewById<EditText>(R.id.etEventTime)

        etEventDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    eventStartCalendar.set(Calendar.YEAR, year)
                    eventStartCalendar.set(Calendar.MONTH, month)
                    eventStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedEventDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(eventStartCalendar.time)
                    etEventDate.setText(selectedEventDate)
                },
                eventStartCalendar.get(Calendar.YEAR),
                eventStartCalendar.get(Calendar.MONTH),
                eventStartCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etEventTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    eventStartCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    eventStartCalendar.set(Calendar.MINUTE, minute)
                    selectedEventTime = SimpleDateFormat("HH:mm", Locale.US).format(eventStartCalendar.time)
                    etEventTime.setText(selectedEventTime)
                },
                eventStartCalendar.get(Calendar.HOUR_OF_DAY),
                eventStartCalendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun publishEvent() {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        val eventName = findViewById<EditText>(R.id.etEventName).text.toString().trim()
        val category = findViewById<Spinner>(R.id.spCategory).selectedItem?.toString().orEmpty()
        val location = findViewById<EditText>(R.id.etLocation).text.toString().trim()
        val capacityText = findViewById<EditText>(R.id.etCapacity).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()
        val ticketPriceText = findViewById<EditText>(R.id.etTicketPrice).text.toString().trim()
        val paymentInfo = findViewById<EditText>(R.id.etPaymentInfo).text.toString().trim()
        val eventDate = listOf(selectedEventDate, selectedEventTime)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (eventName.isEmpty() || category.isEmpty() || location.isEmpty() || capacityText.isEmpty() || description.isEmpty() || selectedEventDate.isEmpty() || selectedEventTime.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data", Toast.LENGTH_SHORT).show()
            return
        }

        val hasHeaderImage = selectedHeaderImageUri != null || existingHeaderImageUrl.isNotBlank()
        val hasGeneralImage = selectedGeneralImageUri != null || existingPosterUrl.isNotBlank()
        if (!hasHeaderImage || !hasGeneralImage) {
            Toast.makeText(this, "Lengkapi gambar header dan gambar umum", Toast.LENGTH_SHORT).show()
            return
        }

        val capacity = capacityText.toIntOrNull()
        if (capacity == null || capacity <= 0) {
            Toast.makeText(this, "Kapasitas harus berupa angka lebih dari 0", Toast.LENGTH_SHORT).show()
            return
        }

        // Capture ticket price: if empty or 0, treat as free event (0)
        val ticketPrice = ticketPriceText.toIntOrNull() ?: 0

        val paymentType =
            if (ticketPrice == 0)
                "FREE"
            else
                "PAID"

        if (paymentType == "PAID" && paymentInfo.isEmpty()) {
            Toast.makeText(
                this,
                "Masukkan informasi pembayaran",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (SupabaseRepository.currentUser(this) == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val user = SupabaseRepository.currentUser(this)
        if (user?.role?.equals("Panitia", ignoreCase = true) != true) {
            Toast.makeText(this, "Hanya akun Panitia yang bisa membuat event", Toast.LENGTH_SHORT).show()
            return
        }

        setSubmitLoading(btnSubmit, true)
        if (isEditMode) {
            SupabaseRepository.updateEvent(
                context = this,
                eventId = editingEventId,
                eventName = eventName,
                category = category,
                location = location,
                capacity = capacity,
                description = description,
                eventDate = eventDate,
                generalImageUri = selectedGeneralImageUri,
                headerImageUri = selectedHeaderImageUri,
                existingPosterUrl = existingPosterUrl,
                existingHeaderImageUrl = existingHeaderImageUrl,
                eventPrice = ticketPrice,
                paymentType = paymentType,
                paymentInfo = paymentInfo
            ) { result ->
                handlePublishResult(result, "Event berhasil diperbarui.")
            }
        } else {
            SupabaseRepository.createEvent(
                context = this,
                eventName = eventName,
                category = category,
                location = location,
                capacity = capacity,
                description = description,
                eventDate = eventDate,
                generalImageUri = selectedGeneralImageUri,
                headerImageUri = selectedHeaderImageUri,
                eventPrice = ticketPrice,
                paymentType = paymentType,
                paymentInfo = paymentInfo
            ) { result ->
                handlePublishResult(result, "Event berhasil dibuat")
            }
        }
    }

    private fun handlePublishResult(result: Result<Unit>, successMessage: String) {
        val btnSubmit = findViewById<Button>(R.id.btnSubmitEvent)
        result
            .onSuccess {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
            .onFailure { exception ->
                showPublishError(exception.message.orEmpty())
                setSubmitLoading(btnSubmit, false)
            }
    }

    private fun ensurePanitiaAccess() {
        val user = SupabaseRepository.currentUser(this) ?: return
        if (!user.role.equals("Panitia", ignoreCase = true)) {
            Toast.makeText(this, "Hanya akun Panitia yang bisa membuat event", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setSubmitLoading(button: Button, loading: Boolean) {
        button.isEnabled = !loading
        button.text = when {
            loading && isEditMode -> "Menyimpan..."
            loading -> "Publishing..."
            isEditMode -> "Simpan Perubahan"
            else -> "Publish Event"
        }
    }

    private fun showSelectedImage(uri: Uri, imageViewId: Int, placeholderId: Int) {
        findViewById<ImageView>(imageViewId).apply {
            setImageURI(uri)
            visibility = View.VISIBLE
        }
        findViewById<LinearLayout>(placeholderId).visibility = View.GONE
    }

    private fun showExistingImage(imageUrl: String, imageViewId: Int, placeholderId: Int) {
        if (imageUrl.isBlank()) return
        findViewById<ImageView>(imageViewId).apply {
            visibility = View.VISIBLE
            Glide.with(this@CreateEventActivity)
                .load(imageUrl)
                .centerCrop()
                .into(this)
        }
        findViewById<LinearLayout>(placeholderId).visibility = View.GONE
    }

    private fun getImageSize(uri: Uri): ImageSize? {
        val boundsSize = getImageSizeFromBounds(uri)
        if (boundsSize != null) return applyExifRotation(uri, boundsSize)

        val headerSize = getImageSizeFromImageDecoder(uri)
        if (headerSize != null) return applyExifRotation(uri, headerSize)

        val decodedSize = getImageSizeFromDecodedBitmap(uri)
        if (decodedSize != null) return applyExifRotation(uri, decodedSize)

        return null
    }

    private fun getImageSizeFromBounds(uri: Uri): ImageSize? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        return ImageSize(width, height)
    }

    private fun getImageSizeFromImageDecoder(uri: Uri): ImageSize? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        return runCatching {
            val source = ImageDecoder.createSource(contentResolver, uri)
            var imageSize: ImageSize? = null
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                imageSize = ImageSize(info.size.width, info.size.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSize(1, 1)
            }
            imageSize
        }.getOrNull()
    }

    private fun getImageSizeFromDecodedBitmap(uri: Uri): ImageSize? {
        return runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)?.let { bitmap ->
                    val size = ImageSize(bitmap.width, bitmap.height)
                    bitmap.recycle()
                    size
                }
            }
        }.getOrNull()
    }

    private fun validateImage(uri: Uri, size: ImageSize?, requirement: ImageRequirement): String? {
        val fileSize = getImageFileSize(uri)
        if (fileSize != null && fileSize > maxImageFileSizeBytes) {
            return "Ukuran file terlalu besar (${formatFileSize(fileSize)}). Maksimal 8 MB."
        }

        if (size == null) {
            return "Orientasi gambar tidak bisa dibaca. Pilih gambar JPG, PNG, atau WebP agar header dan gambar umum tidak tertukar."
        }

        val isLandscape = size.width > size.height
        if (requirement.requiredLandscape && !isLandscape) {
            return "Gambar yang dipilih terlihat tegak. Pilih gambar yang melebar agar tidak tertukar dengan gambar umum."
        }
        if (!requirement.requiredLandscape && isLandscape) {
            return "Gambar yang dipilih terlihat melebar. Pilih gambar yang tegak agar tidak tertukar dengan gambar header."
        }

        if (size.width < requirement.minWidth || size.height < requirement.minHeight) {
            return "Resolusi gambar terlalu kecil (${size.width} x ${size.height} px). Minimal ${requirement.minWidth} x ${requirement.minHeight} px."
        }

        return null
    }

    private fun applyExifRotation(uri: Uri, size: ImageSize): ImageSize {
        val orientation = runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE -> ImageSize(size.height, size.width)
            else -> size
        }
    }

    private fun getImageFileSize(uri: Uri): Long? {
        return runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0 }
            }
        }.getOrNull()
    }

    private fun formatFileSize(bytes: Long): String {
        val megabytes = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", megabytes)
    }

    private fun showImageRequirementDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showPublishError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Gagal publish event")
            .setMessage(message.ifBlank { "Event belum bisa dibuat. Silakan coba lagi." })
            .setPositiveButton("OK", null)
            .show()
    }
}

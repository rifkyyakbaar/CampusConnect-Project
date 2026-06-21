package com.campusconnect.app.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import com.campusconnect.app.model.Event
import com.campusconnect.app.model.Ticket
import com.campusconnect.app.model.Peserta
import com.campusconnect.app.model.Review
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object SupabaseRepository {
    private const val SUPABASE_PROJECT_URL = "https://tnestulrktqmpwwjmoct.supabase.co"
    private const val SUPABASE_REST_URL = "https://tnestulrktqmpwwjmoct.supabase.co/rest/v1"
    private const val SUPABASE_AUTH_URL = "https://tnestulrktqmpwwjmoct.supabase.co/auth/v1"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRuZXN0dWxya3RxbXB3d2ptb2N0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODEwMDI1NDMsImV4cCI6MjA5NjU3ODU0M30.EKEWIFhUdjAUOaSqdVm8ilRevK6qSRsQ9G6uiVrbUok"
    private const val PAYMENT_PROOFS_BUCKET = "payment-proofs"
    private const val EVENT_POSTERS_BUCKET = "event-posters"
    private const val PREF_NAME = "supabase_session"
    private const val AVATARS_BUCKET = "avatars"
    private val mainHandler = Handler(Looper.getMainLooper())

    data class AppUser(
        val uid: String,
        val email: String,
        val fullName: String,
        val role: String,
        val provider: String
    )

    private data class ProfileState(
        val user: AppUser,
        val isDeleted: Boolean
    )

    fun currentUser(context: Context): AppUser? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val uid = prefs.getString("uid", null) ?: return null
        return AppUser(
            uid = uid,
            email = prefs.getString("email", "") ?: "",
            fullName = prefs.getString("fullName", "") ?: "",
            role = prefs.getString("role", "Mahasiswa") ?: "Mahasiswa",
            provider = prefs.getString("provider", "email") ?: "email"
        )
    }

    fun signInWithEmail(
        context: Context,
        email: String,
        password: String,
        callback: (Result<AppUser>) -> Unit
    ) = runAsync(callback) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        val response = runCatching {
            request("POST", "$SUPABASE_AUTH_URL/token?grant_type=password", body)
        }.getOrElse { exception ->
            if (isInvalidLoginCredentialsError(exception)) {
                throw IllegalStateException("Email belum terdaftar atau password salah. Jika belum punya akun, silakan daftar dulu.")
            }
            throw exception
        }
        saveSession(context, response, "email")
        requireUserProfile(context)
    }

    fun signUpWithEmail(
        context: Context,
        fullName: String,
        email: String,
        password: String,
        role: String,
        callback: (Result<AppUser>) -> Unit
    ) = runAsync(callback) {
        val metadata = JSONObject()
            .put("fullName", fullName)
            .put("role", role)
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .put("data", metadata)
        val response = runCatching {
            request("POST", "$SUPABASE_AUTH_URL/signup", body)
        }.getOrElse { exception ->
            if (isEmailAlreadyRegisteredError(exception)) {
                return@runAsync restoreEmailAccountIfDeleted(context, fullName, email, password, role)
            }
            throw exception
        }
        if (response.optJSONObject("user") == null) {
            throw IllegalStateException("Email sudah terdaftar atau registrasi belum bisa diproses. Silakan coba login menggunakan email tersebut.")
        }
        if (isExistingEmailSignupResponse(response)) {
            return@runAsync restoreEmailAccountIfDeleted(context, fullName, email, password, role)
        }
        saveSession(context, response, "email")
        upsertUserProfileOrCurrent(context, fullName, role, "email")
    }

    fun signInWithGoogle(
        context: Context,
        idToken: String,
        fullName: String,
        email: String,
        role: String,
        createProfileIfMissing: Boolean,
        callback: (Result<AppUser>) -> Unit
    ) = runAsync(callback) {
        val body = JSONObject()
            .put("provider", "google")
            .put("id_token", idToken)
        val response = request("POST", "$SUPABASE_AUTH_URL/token?grant_type=id_token", body)
        saveSession(context, response, "google")
        if (createProfileIfMissing) {
            upsertUserProfileOrCurrent(context, fullName.ifBlank { "Pengguna" }, role, "google", email, restoreDeleted = true)
        } else {
            requireUserProfile(context)
        }
    }

    fun loadUserProfile(context: Context, uid: String, callback: (Result<AppUser>) -> Unit) =
        runAsync(callback) {
            val user = fetchUserProfile(context, uid) ?: currentUser(context)
            ?: throw IllegalStateException("User tidak ditemukan.")
            saveUserToPrefs(context, user)
            user
        }

    fun createEvent(
        context: Context,
        eventName: String,
        category: String,
        location: String,
        capacity: Int,
        description: String,
        eventDate: String,
        generalImageUri: Uri? = null,
        headerImageUri: Uri? = null,
        eventPrice: Int = 0,
        paymentType: String,
        paymentInfo: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {
        val user = currentUser(context) ?: throw IllegalStateException("Silakan login terlebih dahulu.")
        val organizer = fetchUserProfile(context, user.uid) ?: user
        val eventId = UUID.randomUUID().toString()
        val posterUrl = generalImageUri?.let {
            uploadEventPoster(context, it, user.uid, eventId, "general")
        }.orEmpty()
        val headerImageUrl = headerImageUri?.let {
            uploadEventPoster(context, it, user.uid, eventId, "header")
        }.orEmpty()
        val body = JSONObject()
            .put("eventId", eventId)
            .put("eventName", eventName)
            .put("category", category)
            .put("location", location)
            .put("capacity", capacity)
            .put("description", description)
            .put("organizerId", user.uid)
            .put("organizerName", organizer.fullName.ifBlank { "Panitia" })
            .put("posterUrl", posterUrl)
            .put("headerImageUrl", headerImageUrl)
            .put("status", "pending")
            .put("eventDate", eventDate)
            .put("registrants", 0)
            .put("eventPrice", eventPrice)
            .put("paymentType", paymentType)
            .put("paymentInfo", paymentInfo)
        request("POST", "$SUPABASE_REST_URL/events", body, bearer = accessToken(context), prefer = "return=minimal")
        Unit
    }

    fun loadPendingEvents(callback: (Result<List<Event>>) -> Unit) = runAsync(callback) {
        val response = request("GET", "$SUPABASE_REST_URL/events?status=eq.pending&order=createdAt.desc")
        parseEvents(response.getJSONArray("data"))
    }

    fun loadAdminEvents(callback: (Result<List<Event>>) -> Unit) = runAsync(callback) {
        val response = request("GET", "$SUPABASE_REST_URL/events?order=createdAt.desc")
        parseEvents(response.getJSONArray("data"))
    }

    fun loadEvents(callback: (Result<List<Event>>) -> Unit) = runAsync(callback) {
        val response = request("GET", "$SUPABASE_REST_URL/events")
        parseEvents(response.getJSONArray("data"))
    }

    fun loadOrganizerEvents(uid: String, callback: (Result<List<Event>>) -> Unit) = runAsync(callback) {
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/events?organizerId=eq.${encode(uid)}&order=createdAt.desc"
        )
        parseEvents(response.getJSONArray("data"))
    }

    fun loadEventById(eventId: String, callback: (Result<Event>) -> Unit) = runAsync(callback) {
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/events?eventId=eq.${encode(eventId)}&limit=1"
        )
        val events = parseEvents(response.getJSONArray("data"))
        events.firstOrNull() ?: throw IllegalStateException("Event tidak ditemukan.")
    }

    private fun incrementEventRegistrants(context: Context, eventId: String) {
        // Gagal increment counter tidak boleh membatalkan tiket yang
        // sudah berhasil dibuat — jadi error di sini hanya di-catch
        // dan diabaikan (tiket tetap valid, hanya angka "tickets left"
        // yang mungkin sedikit meleset kalau RPC gagal).
        runCatching {
            val body = JSONObject().put("p_event_id", eventId)
            request(
                "POST",
                "$SUPABASE_REST_URL/rpc/increment_registrants",
                body,
                bearer = accessToken(context)
            )
        }
    }

    fun updateEventStatus(
        eventId: String,
        status: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {
        if (!status.equals("approved", ignoreCase = true) && !status.equals("rejected", ignoreCase = true)) {
            throw IllegalArgumentException("Status event tidak valid.")
        }

        val body = JSONObject()
            .put("status", status.lowercase(Locale.US))

        request(
            "PATCH",
            "$SUPABASE_REST_URL/events?eventId=eq.${encode(eventId)}",
            body,
            prefer = "return=minimal"
        )

        Unit
    }

    fun createTicket(
        context: Context,
        eventId: String,
        eventName: String,
        category: String,
        eventDate: String,
        eventLocation: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val user = currentUser(context)
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        val ticketId = "CC-" +
                UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .uppercase()

        // PENTING: nama kolom di tabel `tickets` semuanya lowercase
        // polos (ticketid, userid, eventid, dst) — BUKAN camelCase
        // seperti tabel `events`. Field di sini harus persis sama
        // dengan nama kolom di Supabase, kalau tidak PostgREST akan
        // menolak field asing dan insert akan gagal.
        val body = JSONObject()
            .put("ticketid", ticketId)
            .put("userid", user.uid)
            .put("eventid", eventId)
            .put("eventname", eventName)
            .put("category", category)
            .put("eventdate", eventDate)
            .put("eventlocation", eventLocation)
            .put("attendeename", user.fullName)
            .put("attendeerole", user.role)
            .put("status", "CONFIRMED")
            .put("paymentproofurl", "")

        request(
            "POST",
            "$SUPABASE_REST_URL/tickets",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        incrementEventRegistrants(context, eventId)

        Unit
    }

    fun createPaidTicket(
        context: Context,
        eventId: String,
        eventName: String,
        category: String,
        eventDate: String,
        eventLocation: String,
        paymentProofUrl: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val user = currentUser(context)
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        val ticketId =
            "CC-" +
                    UUID.randomUUID()
                        .toString()
                        .substring(0,8)
                        .uppercase()

        val body = JSONObject()
            .put("ticketid", ticketId)
            .put("userid", user.uid)

            .put("eventid", eventId)
            .put("eventname", eventName)
            .put("category", category)
            .put("eventdate", eventDate)
            .put("eventlocation", eventLocation)

            .put("attendeename", user.fullName)
            .put("attendeerole", user.role)

            .put("status", "PENDING")
            .put("paymentproofurl", paymentProofUrl)

        request(
            "POST",
            "$SUPABASE_REST_URL/tickets",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        incrementEventRegistrants(context, eventId)

        Unit
    }

    fun uploadPaymentProofAndCreateTicket(
        context: Context,
        imageUri: Uri,
        eventId: String,
        eventName: String,
        category: String,
        eventDate: String,
        eventLocation: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val user =
            currentUser(context)
                ?: throw IllegalStateException("User tidak ditemukan.")

        val imageUrl =
            uploadPaymentProof(
                context,
                imageUri,
                user.uid
            )

        createPaidTicket(
            context,
            eventId,
            eventName,
            category,
            eventDate,
            eventLocation,
            imageUrl
        ) {

        }

        Unit
    }

    fun loadUserTickets(
        context: Context,
        callback: (Result<List<Ticket>>) -> Unit
    ) = runAsync(callback) {

        val user = currentUser(context)
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        // Hanya tiket CONFIRMED yang tampil di Manage Ticket.
        // PENDING (masih menunggu verifikasi panitia), REJECTED,
        // dan USED (sudah dipakai/dipindah ke History) tidak ikut.
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?userid=eq.${encode(user.uid)}&status=eq.CONFIRMED&order=createdat.desc",
            bearer = accessToken(context)
        )

        val rows = response.getJSONArray("data")
        val tickets = mutableListOf<Ticket>()

        for (i in 0 until rows.length()) {

            val item = rows.getJSONObject(i)

            tickets.add(parseTicket(item))
        }

        tickets
    }

    fun loadHistoryTickets(
        context: Context,
        callback: (Result<List<Ticket>>) -> Unit
    ) = runAsync(callback) {

        val user = currentUser(context)
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        // Tiket yang sudah dipakai (di-scan panitia saat acara)
        // dipindahkan ke History.
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?userid=eq.${encode(user.uid)}&status=eq.USED&order=createdat.desc",
            bearer = accessToken(context)
        )

        val rows = response.getJSONArray("data")
        val tickets = mutableListOf<Ticket>()

        for (i in 0 until rows.length()) {

            val item = rows.getJSONObject(i)

            tickets.add(parseTicket(item))
        }

        tickets
    }

    fun getTicketById(
        context: Context,
        ticketId: String,
        callback: (Result<Ticket>) -> Unit
    ) = runAsync(callback) {

        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?ticketid=eq.${encode(ticketId)}&limit=1",
            bearer = accessToken(context)
        )

        val rows = response.getJSONArray("data")

        if (rows.length() == 0) {
            throw IllegalStateException("Tiket tidak ditemukan.")
        }

        parseTicket(rows.getJSONObject(0))
    }

    fun markTicketUsed(
        context: Context,
        ticketId: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val body = JSONObject()
            .put("status", "USED")

        request(
            "PATCH",
            "$SUPABASE_REST_URL/tickets?ticketid=eq.${encode(ticketId)}",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        Unit
    }

    fun createReview(
        context: Context,
        ticketId: String,
        eventId: String,
        rating: Int,
        comment: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {
        val user = currentUser(context) ?: throw IllegalStateException("Silakan login terlebih dahulu.")
        if (ticketId.isBlank() || eventId.isBlank()) {
            throw IllegalStateException("Data tiket tidak lengkap.")
        }
        if (rating !in 1..5) {
            throw IllegalStateException("Rating harus diisi antara 1 sampai 5.")
        }
        if (hasReviewedSync(context, ticketId)) {
            throw IllegalStateException("Tiket ini sudah pernah direview.")
        }

        val ticket = fetchTicketById(context, ticketId)
        if (ticket.userId != user.uid) {
            throw IllegalStateException("Kamu hanya bisa memberi review untuk tiket milikmu.")
        }
        if (!ticket.eventId.equals(eventId, ignoreCase = true)) {
            throw IllegalStateException("Data event pada tiket tidak sesuai.")
        }
        if (!ticket.status.equals("USED", ignoreCase = true)) {
            throw IllegalStateException("Review hanya bisa diberikan setelah tiket digunakan.")
        }

        val body = JSONObject()
            .put("reviewid", UUID.randomUUID().toString())
            .put("ticketid", ticket.ticketId)
            .put("eventid", ticket.eventId)
            .put("userid", user.uid)
            .put("attendeename", ticket.attendeeName)
            .put("rating", rating)
            .put("comment", comment)

        request(
            "POST",
            "$SUPABASE_REST_URL/reviews",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        Unit
    }

    fun hasReviewed(
        context: Context,
        ticketId: String,
        callback: (Result<Boolean>) -> Unit
    ) = runAsync(callback) {
        if (ticketId.isBlank()) false else hasReviewedSync(context, ticketId)
    }

    fun getReviewByTicketId(
        context: Context,
        ticketId: String,
        callback: (Result<Review?>) -> Unit
    ) = runAsync(callback) {
        if (ticketId.isBlank()) return@runAsync null
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/reviews?ticketid=eq.${encode(ticketId)}&limit=1",
            bearer = accessToken(context)
        )
        val rows = response.getJSONArray("data")
        if (rows.length() == 0) null else parseReview(rows.getJSONObject(0))
    }

    private fun hasReviewedSync(context: Context, ticketId: String): Boolean {
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/reviews?ticketid=eq.${encode(ticketId)}&select=reviewid&limit=1",
            bearer = accessToken(context)
        )
        return response.getJSONArray("data").length() > 0
    }

    private fun fetchTicketById(context: Context, ticketId: String): Ticket {
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?ticketid=eq.${encode(ticketId)}&limit=1",
            bearer = accessToken(context)
        )
        val rows = response.getJSONArray("data")
        if (rows.length() == 0) throw IllegalStateException("Tiket tidak ditemukan.")
        return parseTicket(rows.getJSONObject(0))
    }

    private fun parseTicket(item: JSONObject): Ticket {
        return Ticket(
            ticketId = item.optString("ticketid"),
            userId = item.optString("userid"),
            eventId = item.optString("eventid"),
            eventName = item.optString("eventname"),
            category = item.optString("category"),
            eventDate = item.optString("eventdate"),
            eventLocation = item.optString("eventlocation"),
            attendeeName = item.optString("attendeename"),
            attendeeRole = item.optString("attendeerole"),
            status = item.optString("status"),
            paymentProofUrl = item.optString("paymentproofurl"),
            createdAt = item.optString("createdat")
        )
    }

    private fun parseReview(item: JSONObject): Review {
        return Review(
            reviewId = item.optString("reviewid"),
            ticketId = item.optString("ticketid"),
            eventId = item.optString("eventid"),
            userId = item.optString("userid"),
            attendeeName = item.optString("attendeename"),
            rating = item.optInt("rating"),
            comment = item.optString("comment"),
            createdAt = item.optString("createdat")
        )
    }

    fun loadApprovedEvents(
        callback: (Result<List<Event>>) -> Unit
    ) = runAsync(callback) {

        val response = request(
            "GET",
            "$SUPABASE_REST_URL/events?status=eq.approved"
        )

        parseEvents(response.getJSONArray("data"))
    }

    fun loadParticipants(
        context: Context,
        eventId: String,
        callback: (Result<List<Peserta>>) -> Unit
    ) = runAsync(callback) {

        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?eventid=eq.${encode(eventId)}",
            bearer = accessToken(context)
        )

        val rows = response.getJSONArray("data")

        val participants = mutableListOf<Peserta>()

        for (i in 0 until rows.length()) {

            val item = rows.getJSONObject(i)

            participants.add(
                Peserta(
                    ticketId = item.optString("ticketid"),
                    attendeeName = item.optString("attendeename"),
                    attendeeRole = item.optString("attendeerole"),
                    paymentProofUrl = item.optString("paymentproofurl"),
                    status = item.optString("status")
                )
            )
        }

        participants
    }

    fun approveParticipant(
        context: Context,
        ticketId: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val body = JSONObject()
            .put("status", "CONFIRMED")

        request(
            "PATCH",
            "$SUPABASE_REST_URL/tickets?ticketid=eq.${encode(ticketId)}",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        Unit
    }

    fun rejectParticipant(
        context: Context,
        ticketId: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {

        val body = JSONObject()
            .put("status", "REJECTED")

        request(
            "PATCH",
            "$SUPABASE_REST_URL/tickets?ticketid=eq.${encode(ticketId)}",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        Unit
    }

    fun loadOrganizerStats(uid: String, callback: (Result<Pair<Int, Long>>) -> Unit) =
        runAsync(callback) {
            val response = request("GET", "$SUPABASE_REST_URL/events?organizerId=eq.${encode(uid)}")
            val events = parseEvents(response.getJSONArray("data"))
            events.size to events.sumOf { it.registrants.toLong() }
        }

    fun sendPasswordReset(email: String, callback: (Result<Unit>) -> Unit) = runAsync(callback) {
        val body = JSONObject().put("email", email)
        request("POST", "$SUPABASE_AUTH_URL/recover", body)
        Unit
    }

    fun updateUserNameAndEmail(
        context: Context,
        uid: String,
        newName: String,
        newEmail: String,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {
        val token = accessToken(context)
        if (token.isBlank()) throw IllegalStateException("Sesi login tidak valid. Silakan login ulang.")

        // 1. Update nama di tabel public.users
        val body = JSONObject()
            .put("fullName", newName)

        request(
            "PATCH",
            "$SUPABASE_REST_URL/users?uid=eq.${encode(uid)}",
            body,
            bearer = token,
            prefer = "return=minimal"
        )

        // 2. Jika email diubah, update auth email Supabase (opsional)
        // Jika Anda hanya ingin mengubah nama di database, hapus blok ini.
        val current = currentUser(context)
        if (current != null && current.email != newEmail && current.provider == "email") {
            val emailBody = JSONObject().put("email", newEmail)
            request("PUT", "$SUPABASE_AUTH_URL/user", emailBody, bearer = token)
        }

        // 3. Simpan perubahan ke penyimpanan lokal (SharedPreferences)
        val updatedUser = current?.copy(fullName = newName, email = newEmail)
        if (updatedUser != null) {
            saveUserToPrefs(context, updatedUser)
        }

        Unit
    }

    fun updatePassword(context: Context, newPassword: String, callback: (Result<Unit>) -> Unit) =
        runAsync(callback) {
            val body = JSONObject().put("password", newPassword)
            request("PUT", "$SUPABASE_AUTH_URL/user", body, bearer = accessToken(context))
            Unit
        }

    fun deleteCurrentUser(context: Context, callback: (Result<Unit>) -> Unit) = runAsync(callback) {
        val user = currentUser(context) ?: throw IllegalStateException("User tidak ditemukan.")
        markUserProfileDeleted(context, user.uid)
        runCatching {
            request("DELETE", "$SUPABASE_AUTH_URL/user", bearer = accessToken(context))
        }
        signOut(context)
        Unit
    }

    fun signOut(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun uploadUserAvatar(
        context: Context,
        avatarUri: Uri,
        userId: String,
        callback: (Result<String>) -> Unit
    ) = runAsync(callback) {
        val token = accessToken(context)
        if (token.isBlank()) throw IllegalStateException("Sesi login tidak valid. Silakan login ulang.")

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(avatarUri) ?: "image/jpeg"

        val objectPath = "$userId/avatar.jpg"

        val bytes = contentResolver.openInputStream(avatarUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Gambar tidak bisa dibaca.")

        // Upload file ke bucket
        requestBinary(
            method = "POST",
            url = "$SUPABASE_PROJECT_URL/storage/v1/object/$AVATARS_BUCKET/$objectPath",
            bytes = bytes,
            contentType = mimeType,
            bearer = token,
            upsert = true
        )

        // --- TRIK STEMPEL WAKTU ---
        // Catat waktu upload terakhir ke dalam HP agar URL bisa berubah secara otomatis
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).edit()
            .putLong("avatar_version", System.currentTimeMillis())
            .apply()
        // -------------------------

        getAvatarUrl(context, userId)
    }

    // UBAH FUNGSI INI: Tambahkan parameter `context` untuk membaca stempel waktu
    fun getAvatarUrl(context: Context, userId: String): String {
        val prefs = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
        val version = prefs.getLong("avatar_version", 0) // Ambil stempel waktu

        // Tambahkan ?v=versi di belakang URL untuk menembus cache server
        return "$SUPABASE_PROJECT_URL/storage/v1/object/public/$AVATARS_BUCKET/$userId/avatar.jpg?v=$version"
    }


    private fun requireUserProfile(context: Context): AppUser {
        val current = currentUser(context) ?: throw IllegalStateException("User tidak ditemukan.")
        return fetchUserProfile(context, current.uid)?.also { saveUserToPrefs(context, it) }
            ?: run {
                signOut(context)
                throw IllegalStateException("Profil akun tidak ditemukan. Silakan daftar ulang atau hubungi admin.")
            }
    }

    private fun upsertUserProfileOrCurrent(
        context: Context,
        fullName: String,
        role: String,
        provider: String,
        emailOverride: String? = null,
        restoreDeleted: Boolean = false
    ): AppUser {
        return runCatching {
            upsertUserProfile(context, fullName, role, provider, emailOverride, restoreDeleted)
        }.getOrElse { exception ->
            if (!isMissingUsersTableError(exception)) throw exception
            val current = currentUser(context) ?: throw IllegalStateException("User tidak ditemukan.")
            val fallback = current.copy(
                email = emailOverride?.ifBlank { null } ?: current.email,
                fullName = current.fullName.ifBlank { fullName },
                role = current.role.ifBlank { role },
                provider = provider
            )
            saveUserToPrefs(context, fallback)
            fallback
        }
    }

    private fun upsertUserProfile(
        context: Context,
        fullName: String,
        role: String,
        provider: String,
        emailOverride: String? = null,
        restoreDeleted: Boolean = false
    ): AppUser {
        val current = currentUser(context) ?: throw IllegalStateException("User tidak ditemukan.")
        val email = emailOverride?.ifBlank { null } ?: current.email
        val body = JSONObject()
            .put("uid", current.uid)
            .put("fullName", fullName)
            .put("email", email)
            .put("role", role)
            .put("provider", provider)
            .put("deletedAt", JSONObject.NULL)

        val token = accessToken(context)
        val existing = fetchUserProfileState(context, current.uid)
        if (existing?.isDeleted == true && !restoreDeleted) {
            throw IllegalStateException("Akun ini sudah dihapus dan tidak bisa digunakan untuk login.")
        }
        if (!restoreDeleted && existing?.isDeleted != true) {
            body.remove("deletedAt")
        }
        if (existing == null) {
            request("POST", "$SUPABASE_REST_URL/users", body, bearer = token, prefer = "return=minimal")
        } else {
            request(
                "PATCH",
                "$SUPABASE_REST_URL/users?uid=eq.${encode(current.uid)}",
                body,
                bearer = token,
                prefer = "return=minimal"
            )
        }

        val user = AppUser(current.uid, email, fullName, role, provider)
        saveUserToPrefs(context, user)
        return user
    }

    private fun restoreEmailAccountIfDeleted(
        context: Context,
        fullName: String,
        email: String,
        password: String,
        role: String
    ): AppUser {
        val response = runCatching {
            request(
                "POST",
                "$SUPABASE_AUTH_URL/token?grant_type=password",
                JSONObject().put("email", email).put("password", password)
            )
        }.getOrElse { exception ->
            if (isInvalidLoginCredentialsError(exception)) {
                throw IllegalStateException("Email pernah terdaftar. Gunakan password lama atau reset password terlebih dahulu untuk membuat ulang akun.")
            }
            throw exception
        }

        saveSession(context, response, "email")
        val current = currentUser(context) ?: throw IllegalStateException("User tidak ditemukan.")
        val profile = fetchUserProfileState(context, current.uid)
        if (profile != null && !profile.isDeleted) {
            signOut(context)
            throw IllegalStateException("Email sudah terdaftar. Silakan login menggunakan email tersebut.")
        }

        return upsertUserProfileOrCurrent(
            context = context,
            fullName = fullName,
            role = role,
            provider = "email",
            emailOverride = email,
            restoreDeleted = true
        )
    }

    private fun fetchUserProfile(context: Context, uid: String): AppUser? {
        val state = fetchUserProfileState(context, uid) ?: return null
        if (state.isDeleted) {
            signOut(context)
            throw IllegalStateException("Akun ini sudah dihapus dan tidak bisa digunakan untuk login.")
        }
        return state.user
    }

    private fun fetchUserProfileState(context: Context, uid: String): ProfileState? {
        val response = runCatching {
            request(
                "GET",
                "$SUPABASE_REST_URL/users?uid=eq.${encode(uid)}&limit=1",
                bearer = accessToken(context)
            )
        }.getOrElse { exception ->
            if (isMissingUsersTableError(exception)) return null
            throw exception
        }
        val rows = response.getJSONArray("data")
        if (rows.length() == 0) return null
        val item = rows.getJSONObject(0)
        return ProfileState(
            user = AppUser(
                uid = item.optString("uid", uid),
                email = item.optString("email", ""),
                fullName = item.optString("fullName", "Pengguna"),
                role = item.optString("role", "Mahasiswa"),
                provider = item.optString("provider", "email")
            ),
            isDeleted = isDeletedProfile(item)
        )
    }

    private fun markUserProfileDeleted(context: Context, uid: String) {
        val token = accessToken(context)
        if (token.isBlank()) throw IllegalStateException("Sesi login tidak valid. Silakan login ulang.")

        val existingProfile = runCatching {
            fetchUserProfileForAccountDeletion(token, uid)
        }.getOrElse { exception ->
            if (isMissingUsersTableError(exception)) return
            throw exception
        }

        if (existingProfile == null) return

        val body = JSONObject()
            .put("fullName", "")
            .put("email", existingProfile.email)
            .put("role", "Deleted")
            .put("provider", existingProfile.provider)
            .put("deletedAt", currentTimestamp())

        val response = request(
            "PATCH",
            "$SUPABASE_REST_URL/users?uid=eq.${encode(uid)}",
            body,
            bearer = token,
            prefer = "return=representation"
        )
        val updatedRows = response.optJSONArray("data") ?: JSONArray()
        if (updatedRows.length() == 0) {
            throw IllegalStateException("Profil users tidak diperbarui. Pastikan policy UPDATE public.users mengizinkan user menghapus akunnya sendiri.")
        }
    }

    private fun fetchUserProfileForAccountDeletion(token: String, uid: String): AppUser? {
        val response = request(
            "GET",
            "$SUPABASE_REST_URL/users?uid=eq.${encode(uid)}&limit=1",
            bearer = token
        )
        val rows = response.getJSONArray("data")
        if (rows.length() == 0) return null
        val item = rows.getJSONObject(0)
        return AppUser(
            uid = item.optString("uid", uid),
            email = item.optString("email", ""),
            fullName = item.optString("fullName", "Pengguna"),
            role = item.optString("role", "Mahasiswa"),
            provider = item.optString("provider", "email")
        )
    }

    private fun saveSession(context: Context, response: JSONObject, provider: String) {
        val user = response.optJSONObject("user")
            ?: throw IllegalStateException("Sesi login tidak valid. Silakan coba login ulang.")
        val accessToken = response.optString("access_token")
        if (accessToken.isBlank()) {
            throw IllegalStateException("Registrasi berhasil. Silakan cek email untuk verifikasi, lalu login.")
        }
        val metadata = user.optJSONObject("user_metadata")
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("accessToken", accessToken)
            .putString("refreshToken", response.optString("refresh_token"))
            .putString("uid", user.getString("id"))
            .putString("email", user.optString("email"))
            .putString("fullName", metadata?.optString("fullName") ?: metadata?.optString("full_name") ?: "")
            .putString("role", metadata?.optString("role") ?: "Mahasiswa")
            .putString("provider", provider)
            .apply()
    }

    private fun saveUserToPrefs(context: Context, user: AppUser) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("uid", user.uid)
            .putString("email", user.email)
            .putString("fullName", user.fullName)
            .putString("role", user.role)
            .putString("provider", user.provider)
            .apply()
    }

    private fun parseEvents(rows: JSONArray): List<Event> {
        val events = mutableListOf<Event>()
        for (index in 0 until rows.length()) {
            val item = rows.getJSONObject(index)
            events.add(
                Event(
                    id = item.optString("eventId", item.optString("id", "")),
                    eventName = item.optString("eventName", ""),
                    category = item.optString("category", ""),
                    location = item.optString("location", ""),
                    description = item.optString("description", ""),
                    organizerId = item.optString("organizerId", ""),
                    organizerName = item.optString("organizerName", ""),
                    capacity = item.optInt("capacity", 0),
                    registrants = item.optInt("registrants", 0),
                    status = item.optString("status", "pending"),
                    posterUrl = item.optString("posterUrl", ""),
                    headerImageUrl = item.optString("headerImageUrl", ""),
                    eventDate = item.optString("eventDate", ""),
                    createdAt = if (item.isNull("createdAt")) null else item.optString("createdAt"),
                    eventPrice = item.optInt("eventPrice", 0),
                    paymentType = item.optString("paymentType", "FREE"),
                    paymentInfo = item.optString("paymentInfo", "")
                )
            )
        }
        return events
    }

    private fun uploadEventPoster(
        context: Context,
        posterUri: Uri,
        userId: String,
        eventId: String,
        imageType: String
    ): String {
        val token = accessToken(context)
        if (token.isBlank()) throw IllegalStateException("Sesi login tidak valid. Silakan login ulang.")

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(posterUri) ?: "image/jpeg"
        if (!mimeType.startsWith("image/")) {
            throw IllegalStateException("File poster harus berupa gambar.")
        }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?.takeIf { it.isNotBlank() }
            ?: "jpg"
        val objectPath = "$userId/$eventId-$imageType.$extension"
        val bytes = contentResolver.openInputStream(posterUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Gambar poster tidak bisa dibaca.")

        runCatching {
            requestBinary(
                method = "POST",
                url = "$SUPABASE_PROJECT_URL/storage/v1/object/$EVENT_POSTERS_BUCKET/$objectPath",
                bytes = bytes,
                contentType = mimeType,
                bearer = token,
                upsert = false
            )
        }.getOrElse { exception ->
            if (isMissingEventPostersBucketError(exception)) {
                throw IllegalStateException(
                    "Bucket Storage '$EVENT_POSTERS_BUCKET' belum ada. Jalankan SQL setup Supabase untuk membuat bucket poster event."
                )
            }
            throw exception
        }

        return "$SUPABASE_PROJECT_URL/storage/v1/object/public/$EVENT_POSTERS_BUCKET/$objectPath"
    }

    private fun uploadPaymentProof(
        context: Context,
        imageUri: Uri,
        userId: String
    ): String {

        val token = accessToken(context)

        if (token.isBlank())
            throw IllegalStateException("Silakan login ulang.")

        val mimeType =
            context.contentResolver.getType(imageUri)
                ?: "image/jpeg"

        val extension =
            MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "jpg"

        val objectPath =
            "$userId/${UUID.randomUUID()}.$extension"

        val bytes =
            context.contentResolver
                .openInputStream(imageUri)
                ?.use { it.readBytes() }
                ?: throw IllegalStateException("Gambar tidak bisa dibaca.")

        requestBinary(
            method = "POST",
            url =
                "$SUPABASE_PROJECT_URL/storage/v1/object/$PAYMENT_PROOFS_BUCKET/$objectPath",

            bytes = bytes,
            contentType = mimeType,
            bearer = token,
            upsert = false
        )

        return "$SUPABASE_PROJECT_URL/storage/v1/object/public/$PAYMENT_PROOFS_BUCKET/$objectPath"
    }

    private fun requestBinary(
        method: String,
        url: String,
        bytes: ByteArray,
        contentType: String,
        bearer: String,
        upsert: Boolean
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $bearer")
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("x-upsert", upsert.toString())
            outputStream.use { it.write(bytes) }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = parseErrorMessage(text)
            throw IllegalStateException(message.ifBlank { "Upload poster gagal ($status)." })
        }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun request(
        method: String,
        url: String,
        body: JSONObject? = null,
        bearer: String? = null,
        prefer: String? = null
    ): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${bearer?.takeIf { it.isNotBlank() } ?: SUPABASE_ANON_KEY}")
            setRequestProperty("Content-Type", "application/json")
            prefer?.let { setRequestProperty("Prefer", it) }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }

        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            val message = parseErrorMessage(text)
            throw IllegalStateException(message.ifBlank { "Request Supabase gagal ($status)." })
        }
        return if (text.isBlank()) JSONObject() else {
            val trimmed = text.trim()
            if (trimmed.startsWith("[")) JSONObject().put("data", JSONArray(trimmed)) else JSONObject(trimmed)
        }
    }

    private fun accessToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString("accessToken", "") ?: ""
        val refreshToken = prefs.getString("refreshToken", "") ?: ""

        // Coba refresh token jika access token terlihat kedaluwarsa.
        // Supabase JWT body (bagian tengah base64) berisi field "exp" (unix timestamp).
        // Kita decode dan bandingkan dengan waktu sekarang.
        if (token.isNotBlank() && refreshToken.isNotBlank() && isTokenExpiredOrNearExpiry(token)) {
            return runCatching {
                refreshAccessToken(context, refreshToken)
            }.getOrDefault(token) // Jika refresh gagal, kembalikan token lama agar error asli tetap terbaca
        }

        return token
    }

    /**
     * Cek apakah JWT access token sudah expired atau akan expired dalam 60 detik ke depan.
     * JWT terdiri dari 3 bagian dipisah titik: header.payload.signature
     * Payload di-encode base64url — decode untuk membaca field "exp".
     */
    private fun isTokenExpiredOrNearExpiry(token: String): Boolean {
        return runCatching {
            val parts = token.split(".")
            if (parts.size != 3) return true
            // base64url decode — ganti - dengan + dan _ dengan /
            val payload = parts[1]
                .replace("-", "+")
                .replace("_", "/")
                .let { padded ->
                    // Tambahkan padding '=' jika perlu
                    val pad = padded.length % 4
                    if (pad == 0) padded else padded + "=".repeat(4 - pad)
                }
            val decoded = String(android.util.Base64.decode(payload, android.util.Base64.DEFAULT))
            val json = org.json.JSONObject(decoded)
            val exp = json.optLong("exp", 0L)
            val nowPlusBUffer = System.currentTimeMillis() / 1000L + 60L // 60 detik buffer
            exp < nowPlusBUffer
        }.getOrDefault(false)
    }

    /**
     * Panggil endpoint refresh token Supabase, simpan sesi baru, kembalikan access token baru.
     * Dipanggil secara sinkron karena accessToken() sendiri dipanggil dari background thread.
     */
    private fun refreshAccessToken(context: Context, refreshToken: String): String {
        val body = JSONObject()
            .put("refresh_token", refreshToken)
        val response = request(
            "POST",
            "$SUPABASE_AUTH_URL/token?grant_type=refresh_token",
            body
        )
        val newAccessToken = response.optString("access_token")
        val newRefreshToken = response.optString("refresh_token")
        if (newAccessToken.isNotBlank()) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putString("accessToken", newAccessToken)
                .putString("refreshToken", newRefreshToken.ifBlank { refreshToken })
                .apply()
        }
        return newAccessToken.ifBlank { refreshToken }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun isDeletedProfile(item: JSONObject): Boolean {
        return !item.isNull("deletedAt") || item.optString("role").equals("Deleted", ignoreCase = true)
    }

    private fun parseErrorMessage(text: String): String {
        if (text.isBlank()) return ""
        return runCatching {
            val json = JSONObject(text)
            listOf("msg", "message", "error_description", "error", "code")
                .firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
                ?: text
        }.getOrDefault(text)
    }

    private fun isMissingUsersTableError(exception: Throwable): Boolean {
        val message = exception.localizedMessage.orEmpty()
        return message.contains("public.users", ignoreCase = true) &&
                message.contains("schema cache", ignoreCase = true)
    }

    private fun isInvalidLoginCredentialsError(exception: Throwable): Boolean {
        val message = exception.localizedMessage.orEmpty()
        return message.contains("invalid login credentials", ignoreCase = true) ||
                message.contains("invalid_grant", ignoreCase = true)
    }

    private fun isEmailAlreadyRegisteredError(exception: Throwable): Boolean {
        val message = exception.localizedMessage.orEmpty()
        return message.contains("already registered", ignoreCase = true) ||
                message.contains("already exists", ignoreCase = true) ||
                message.contains("user already", ignoreCase = true)
    }

    private fun isExistingEmailSignupResponse(response: JSONObject): Boolean {
        val user = response.optJSONObject("user") ?: return false
        val identities = user.optJSONArray("identities") ?: return false
        return identities.length() == 0
    }

    private fun isMissingEventPostersBucketError(exception: Throwable): Boolean {
        val message = exception.localizedMessage.orEmpty()
        return message.contains("bucket not found", ignoreCase = true) ||
                (message.contains(EVENT_POSTERS_BUCKET, ignoreCase = true) &&
                        message.contains("not found", ignoreCase = true))
    }

    private fun <T> runAsync(callback: (Result<T>) -> Unit, block: () -> T) {
        Thread {
            val result = runCatching(block)
            mainHandler.post { callback(result) }
        }.start()
    }
}

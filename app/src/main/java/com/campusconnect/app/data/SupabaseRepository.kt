package com.campusconnect.app.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import com.campusconnect.app.model.Event
import com.campusconnect.app.model.Ticket
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
    private const val EVENT_POSTERS_BUCKET = "event-posters"
    private const val PREF_NAME = "supabase_session"
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
        posterUri: Uri? = null,
        eventPrice: Int = 0,
        callback: (Result<Unit>) -> Unit
    ) = runAsync(callback) {
        val user = currentUser(context) ?: throw IllegalStateException("Silakan login terlebih dahulu.")
        val organizer = fetchUserProfile(context, user.uid) ?: user
        val eventId = UUID.randomUUID().toString()
        val posterUrl = posterUri?.let {
            uploadEventPoster(context, it, user.uid, eventId)
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
            .put("status", "pending")
            .put("eventDate", eventDate)
            .put("registrants", 0)
            .put("eventPrice", eventPrice)
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

        val body = JSONObject()
            .put("ticketId", ticketId)
            .put("userId", user.uid)
            .put("eventId", eventId)
            .put("eventName", eventName)
            .put("category", category)
            .put("eventDate", eventDate)
            .put("eventLocation", eventLocation)
            .put("attendeeName", user.fullName)
            .put("attendeeRole", user.role)
            .put("status", "Confirmed")

        request(
            "POST",
            "$SUPABASE_REST_URL/tickets",
            body,
            bearer = accessToken(context),
            prefer = "return=minimal"
        )

        Unit
    }

    fun loadUserTickets(
        context: Context,
        callback: (Result<List<Ticket>>) -> Unit
    ) = runAsync(callback) {

        val user = currentUser(context)
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        val response = request(
            "GET",
            "$SUPABASE_REST_URL/tickets?userId=eq.${encode(user.uid)}&order=createdAt.desc",
            bearer = accessToken(context)
        )

        val rows = response.getJSONArray("data")
        val tickets = mutableListOf<Ticket>()

        for (i in 0 until rows.length()) {

            val item = rows.getJSONObject(i)

            tickets.add(
                Ticket(
                    ticketId = item.optString("ticketId"),
                    userId = item.optString("userId"),

                    eventId = item.optString("eventId"),
                    eventName = item.optString("eventName"),
                    category = item.optString("category"),
                    eventDate = item.optString("eventDate"),
                    eventLocation = item.optString("eventLocation"),

                    attendeeName = item.optString("attendeeName"),
                    attendeeRole = item.optString("attendeeRole"),

                    status = item.optString("status"),

                    createdAt = item.optString("createdAt")
                )
            )
        }

        tickets
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
                    eventDate = item.optString("eventDate", ""),
                    createdAt = if (item.isNull("createdAt")) null else item.optString("createdAt"),
                    eventPrice = item.optInt("eventPrice", 0)
                )
            )
        }
        return events
    }

    private fun uploadEventPoster(context: Context, posterUri: Uri, userId: String, eventId: String): String {
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
        val objectPath = "$userId/$eventId.$extension"
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
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("accessToken", "") ?: ""
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

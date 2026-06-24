# CampusConnect🎓 

**CampusConnect** adalah aplikasi Android berbasis *serverless cloud* yang dirancang untuk menjadi pusat ekosistem kegiatan kampus. Aplikasi ini memecahkan masalah fragmentasi informasi dengan memudahkan mahasiswa untuk mencari dan mendaftar acara, sekaligus mendigitalisasi alur kerja panitia melalui manajemen tiket digital dan presensi berbasis QR Code.

Proyek ini dibangun menggunakan arsitektur **MVVM (Model-View-ViewModel)** dengan mengintegrasikan **Supabase** sebagai *Backend-as-a-Service* (BaaS) untuk menjamin skalabilitas dan performa *database* relasional.

---

## ✨ Fitur Utama

Sistem CampusConnect membagi otorisasi akses ke dalam 3 Peran (Role) dengan fitur spesifik:

### 👤 Mahasiswa (Peserta)
* **Katalog Event Terpusat:** Eksplorasi daftar *event* kampus terbaru berdasarkan kategori (Seminar, Workshop, Lomba, dll).
* **Pendaftaran & Pembayaran:** Alur pendaftaran terintegrasi dengan fitur unggah bukti transfer untuk *event* berbayar.
* **Dompet Tiket Digital:** Mendapatkan *QR Code* unik sebagai tiket masuk *check-in* di hari H.
* **Sistem Notifikasi:** Pembaruan *real-time* mengenai status persetujuan tiket atau perubahan jadwal acara.
* **Sistem Ulasan (Post-Event):** Memberikan *rating* dan *feedback* setelah acara selesai diikuti.

### 🎫 Panitia (Penyelenggara)
* **Manajemen Event (CRUD):** Pengajuan *event* baru lengkap dengan unggahan poster acara, pengaturan kuota, dan harga tiket.
* **Verifikasi Peserta:** Dasbor untuk meninjau bukti pembayaran dan melakukan *Approve/Reject* tiket pendaftar.
* **QR Code Scanner:** Pemindai bawaan aplikasi untuk melakukan *check-in* presensi peserta di lokasi acara secara instan dan akurat.

### 🛡️ Admin (Pengawas)
* **Verifikasi Event:** Sistem *Approve/Reject* pengajuan acara dari panitia sebelum publikasi untuk menjaga kualitas konten aplikasi.
* **Manajemen Pengguna:** Dasbor kontrol untuk memantau dan mengelola (*Ban/Unban*) akun pengguna.

---

## 🛠️ Arsitektur & Teknologi

Aplikasi ini menerapkan prinsip *Clean Code* dengan pemisahan lapisan (UI Logic, Data, dan Network) menggunakan teknologi terkini:

* **Arsitektur:** MVVM (Model-View-ViewModel)
* **Bahasa Pemrograman:** Kotlin
* **Antarmuka (UI/UX):** XML dengan penerapan desain *Glassmorphism*, *Neon Accents*, dan Material Design Components.
* **Asynchronous Programming:** Kotlin Coroutines & Flow
* **Backend & Database (Supabase):**
  * **PostgreSQL:** *Database* relasional untuk manajemen pengguna, *event*, dan tiket.
  * **Supabase Auth:** Autentikasi pengguna terenkripsi.
  * **Supabase Storage:** Manajemen unggahan *file* (gambar *header*, poster umum, dan bukti pembayaran).
* **Library Utama:**
  * `Retrofit` / `Ktor` - Komunikasi API
  * `Glide` / `Coil` - *Image Loading & Caching*
  * `ZXing Scanner` - Integrasi *QR Code Reader*
* **Version Control & Desain:** GitHub, Figma

---

## 👥 Tim Pengembang

Proyek ini merupakan hasil kolaborasi tim dengan pembagian tugas lintas disiplin (*Agile Development*):

| Nama Anggota | Peran (Role) | Fokus Tugas |
| :--- | :--- | :--- |
| **Rifky Akbar Utomo Putra** | Project Manager, UI/UX Designer & Frontend Developer | Merancang antarmuka (Figma), manajemen proyek, *Slicing* XML, UI Logic (Kotlin), arsitektur MVVM, dan integrasi komponen *frontend*. |
| **Yurian Fathur Fajar** | Frontend Developer | *Slicing* XML, implementasi UI Logic (Kotlin), dan *binding* data ke antarmuka aplikasi. |
| **Naufal Pramudya Ananda** | Backend Developer 1 | Konfigurasi Supabase Auth, Session Management, dan pembentukan *endpoint* API jaringan. |
| **Wadis Friendly** | Backend Developer 2 | Merancang arsitektur relasi tabel PostgreSQL Supabase, implementasi RLS (Row Level Security), dan fungsi logika CRUD *database*. |

---

## 🚀 Instalasi & Konfigurasi (*Local Setup*)

Untuk menjalankan proyek ini di mesin lokal, ikuti langkah-langkah berikut:

1. **Clone Repositori:**
```bash
   git clone [https://github.com/USERNAME-KAMU/CampusConnect-Project.git](https://github.com/USERNAME-KAMU/CampusConnect-Project.git)

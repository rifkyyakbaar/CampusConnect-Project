# CampusConnect-Project

**CampusConnect** adalah aplikasi Android berbasis *cloud* yang dirancang untuk memudahkan mahasiswa dan panitia dalam mencari, mencatat, dan mengelola acara (event) di lingkungan kampus. Proyek ini dikembangkan secara kolaboratif menggunakan arsitektur *Agile* dengan integrasi Supabase sebagai *Backend as a Service* (BaaS).

## ✨ Fitur Utama
* **Autentikasi Pengguna:** Sistem Login dan Register yang aman menggunakan Supabase Auth.
* **Manajemen Event (CRUD):** Fitur bagi panitia untuk menambahkan (Create), melihat (Read), mengubah (Update), dan menghapus (Delete) data event kampus.
* **Dashboard Berbasis Cloud:** Daftar event dan data pengguna tersimpan di database Supabase.
* **Role-Based Access:** Pembatasan akses keamanan (Security Rules) antara akun mahasiswa biasa dan akun panitia penyelenggara.

## 🛠️ Teknologi yang Digunakan
* **Frontend:** Kotlin, XML, Android SDK
* **Komponen UI:** RecyclerView, Material Design, CardView
* **Backend & Database:** * Supabase Auth
  * Supabase REST API
  * Supabase PostgreSQL Database
* **Tools:** Figma (UI/UX Design), GitHub (Version Control)

## 👥 Tim Pengembang (Kelompok)
Proyek ini dikembangkan oleh 4 orang dengan pembagian peran yang spesifik:

| Nama Anggota | Peran (Role) | Fokus Tugas |
| :--- | :--- | :--- |
| **Rifky Akbar Utomo Putra** | Project Manager, UI/UX Designer & Frontend Developer | Merancang antarmuka (Figma), manajemen proyek, QA, Slicing XML, UI Logic (Kotlin), dan integrasi komponen |
| **Yurian Fathur Fajar** | Frontend Developer | Slicing XML, UI Logic (Kotlin), dan integrasi komponen |
| **Naufal Pramudya Ananda** | Backend Developer 1 | Supabase Auth, Session Management, dan akses API |
| **Wadis Friendly** | Backend Developer 2 | Supabase database architecture dan fungsi logika CRUD |

## 🚀 Instalasi & Konfigurasi
1. *Clone repository* ini ke komputer lokal:
   ```bash
   git clone [https://github.com/USERNAME-KAMU/CampusConnect-Project.git](https://github.com/USERNAME-KAMU/CampusConnect-Project.git)

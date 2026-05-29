package com.example.db

import kotlinx.coroutines.flow.Flow

class UpdateRepository(private val database: AppDatabase) {
    private val checklistDao = database.checklistItemDao()
    private val logDao = database.appLogDao()

    val allChecklistItems: Flow<List<ChecklistItem>> = checklistDao.getAllItems()
    val allLogs: Flow<List<AppLog>> = logDao.getAllLogs()

    suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistDao.updateItem(item)
    }

    suspend fun addLog(message: String, type: String) {
        logDao.insertLog(AppLog(message = message, type = type))
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    suspend fun initializeChecklistIfNeeded() {
        // Define the master list of all guide checklists for update, privacy/security, and battery optimization
        val masterList = listOf(
            // 1. UPDATE PREPARATION (IDs 1-9)
            ChecklistItem(
                id = 1,
                title = "Cadangkan Data HP Anda",
                description = "Simpan cadangan kontak, pesan, foto penting, dan data WhatsApp ke akun Google Cloud sebelum melakukan pengerjaan apa pun.",
                isChecked = false
            ),
            ChecklistItem(
                id = 2,
                title = "Siapkan Ruang Penyimpanan Bebas (Min. 5 GB)",
                description = "Paket sistem ColorOS 12.1 berukuran sekitar 3.20 GB. Dibutuhkan setidaknya 5 GB ruang kosong agar proses ekstraksi file berjalan mulus.",
                isChecked = false
            ),
            ChecklistItem(
                id = 3,
                title = "Isi Baterai hingga Minimal 55% / Sambil Dicas",
                description = "Siklus pemasangan patch baru memakan daya yang sangat tinggi. Sangat disarankan menyambungkan pengisi daya selama proses berlangsung.",
                isChecked = false
            ),
            ChecklistItem(
                id = 4,
                title = "Hubungkan ke Jaringan Wi-Fi Stabil",
                description = "Gunakan Wi-Fi berkecepatan tinggi daripada data seluler untuk menghindari korupsi data (checksum mismatch) saat mengunduh berkas firmware.",
                isChecked = false
            ),
            ChecklistItem(
                id = 5,
                title = "Nonaktifkan Sementara Penghemat Daya",
                description = "Supaya proses download dan kompilasi modul sistem di latar belakang tidak tertidur atau tertunda secara otomatis.",
                isChecked = false
            ),

            // 2. PRIVACY & SECURITY RECOMMENDATIONS (IDs 10-19)
            ChecklistItem(
                id = 10,
                title = "Tinjau Modul 'Google Privacy Dashboard'",
                description = "Ulas aplikasi yang mengakses lokasi, kamera, dan mikrofon dalam 24 jam terakhir lewat menu Pengaturan > Privasi > Dasbor Privasi.",
                isChecked = false
            ),
            ChecklistItem(
                id = 11,
                title = "Aktifkan Indikator Akses Kamera & Mikrofon",
                description = "Aktifkan sakelar privasi agar ikon titik hijau menyala seketika saat ada aplikasi yang diam-diam membuka kamera atau mikrofon ponsel.",
                isChecked = false
            ),
            ChecklistItem(
                id = 12,
                title = "Batasi Izin Lokasi Presisi ke Lokasi Perkiraan",
                description = "Alihkan izin pelacakan lokasi ke koordinat perkiraan (Approximate Location) di Pengaturan > Izin untuk aplikasi non-esensial demi menjaga privasi.",
                isChecked = false
            ),
            ChecklistItem(
                id = 13,
                title = "Setel Brankas Pribadi Aman (Private Safe)",
                description = "Gunakan sandi privasi khusus untuk menyimpan foto sensitif, data identitas, atau dokumen penting Anda secara terenkripsi dalam Brankas Pribadi Oppo.",
                isChecked = false
            ),
            ChecklistItem(
                id = 14,
                title = "Nyalakan Proteksi Transaksi & Pembayaran",
                description = "Buka aplikasi Manajer Telepon bawaan Oppo, pilih Proteksi Pembayaran untuk menjamin keamanan aplikasi perbankan m-banking dan dompet digital.",
                isChecked = false
            ),

            // 3. BATTERY OPTIMIZATIONS (IDs 20-29)
            ChecklistItem(
                id = 20,
                title = "Aktifkan 'Pengisian Daya Malam Teroptimalkan'",
                description = "Pengaturan > Baterai > Pengaturan Lainnya > Hidupkan fitur ini untuk memperlambat arus setelah 80% saat Anda tertidur demi memperpanjang umur baterai.",
                isChecked = false
            ),
            ChecklistItem(
                id = 21,
                title = "Konfigurasikan Mode Daya Hemat Cerdas",
                description = "Pengaturan > Baterai > Aktifkan Mode Hemat Daya untuk memangkas aktivitas sync latar belakang otomatis dan kecerahan layar ketika sisa baterai kritis.",
                isChecked = false
            ),
            ChecklistItem(
                id = 22,
                title = "Batasi Autostart Aplikasi di Latar Belakang",
                description = "Gunakan Manajer Telepon > Manajemen Aplikasi > Luncur Otomatis. Matikan aplikasi pesan instan sekunder atau e-commerce dari autorun.",
                isChecked = false
            ),
            ChecklistItem(
                id = 23,
                title = "Bekukan Aplikasi Boros Daya (Quick App Freeze)",
                description = "Pengaturan > Baterai > Manajemen Energi Aplikasi, lalu pilih 'Bekukan Cepat' pada aplikasi game atau belanja online yang jarang dipakai agar tidak boros baterai.",
                isChecked = false
            ),
            ChecklistItem(
                id = 24,
                title = "Kurangi Durasi Waktu Tunggu Layar Aktif",
                description = "Sinar layar IPS LCD memakan porsi baterai terbesar. Atur waktu penguncian otomatis menjadi 15 atau 30 detik saja di Pengaturan > Layar & Kecerahan.",
                isChecked = false
            )
        )

        val existingItems = checklistDao.getAllItemsList()
        val existingIds = existingItems.map { it.id }.toSet()
        val missingItems = masterList.filter { it.id !in existingIds }

        if (existingItems.isEmpty()) {
            checklistDao.insertAll(masterList)
            addLog("Berhasil menginisialisasi 15 item panduan checklist update, privasi, dan daya baterai.", "INFO")
        } else if (missingItems.isNotEmpty()) {
            checklistDao.insertAll(missingItems)
            addLog("Berhasil menambahkan ${missingItems.size} rekomendasi panduan baru ke database pendukung.", "INFO")
        }
    }
}

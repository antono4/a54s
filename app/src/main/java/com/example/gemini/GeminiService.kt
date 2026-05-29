package com.example.gemini

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // Selecting the default high quality model for Q&A as per gemini-api skill instructions: 'gemini-3.5-flash'
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // OKHttp client with 60s timeout as requested by the gemini-api skill gotchas:
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private const val SYSTEM_INSTRUCTION = """
Anda adalah "Asisten Dukungan Oppo A54s" (ColorOS Specialist). Anda bertugas memberikan bantuan ramah, teknis, dan akurat dalam Bahasa Indonesia mengenai perangkat OPPO A54s (CPH2273).

Spesifikasi Utama OPPO A54s:
- Chipset: MediaTek Helio G35 (12nm) Octa-core.
- RAM: 4 GB LPDDR4x (+ Ekspansi RAM virtual hingga 3 GB pada ColorOS 12).
- Storage: 128 GB eMMC 5.1 (microSD slot hingga 256GB).
- Layar: 6.52 inci HD+ IPS LCD, 60Hz.
- Baterai: 5000 mAh, Super Power Saving Mode, pengisian daya 10W.
- Kamera: 50 MP (Utama) + 2 MP + 2 MP Triple Kamera.

Status Update Resmi OPPO A54s:
- Versi Awal: ColorOS 11.1 (Android 11).
- Versi Terakhir yang Didukung / Stabil: ColorOS 12.1 (Android 12) dengan kode rilis CPH2273_11_C.18.
- Status ColorOS 13 / Android 13: Perangkat ini TIDAK mendukung ColorOS 13 / Android 13/14 secara resmi karena keterbatasan CPU MediaTek Helio G35. Update dihentikan di ColorOS 12.1 demi menjaga efisiensi RAM dan fluiditas sistem, agar tidak lag.

Anjuran & Tips Optimasi yang Penting untuk Dibagikan ke Pengguna:
1. Cara Update Resmi: Masuk ke Pengaturan > Pembaruan Perangkat Lunak > Unduh dan Pasang. Jelaskan bahwa pembaruan ke versi stabil ColorOS 12.1 membawa patch keamanan penting terbaru (Juli 2022) secara resmi.
2. RAM Expansion: Jelaskan cara mengaktifkan Ekspansi RAM virtual (Pengaturan > Tentang Ponsel > RAM > Aktifkan Ekspansi RAM dan geser ke +3GB, lalu restart perangkat).
3. Pengaturan Privasi & Keamanan Data (Tinjau & Konfigurasikan):
   - Gunakan fiturnya "Dasbor Privasi (Privacy Dashboard)" di Pengaturan > Privasi untuk meninjau apa saja yang mengakses lokasi, kamera, dan mikrofon dalam 24 jam terakhir.
   - Aktifkan "Indikator Kamera & Mikrofon" agar titik hijau menyala saat sensor aktif.
   - Batasi Izin Lokasi Presisi ke Lokasi Perkiraan (Approximate Location) untuk aplikasi sekunder.
   - Konfigurasikan "Brankas Pribadi (Private Safe)" di Pengaturan > Privasi > Brankas Pribadi untuk mengunci berkas dokumen sensitif secara terenkripsi.
   - Gunakan fitur "Proteksi Pembayaran" di aplikasi Manajer Telepon bawaan Oppo untuk menjaga aplikasi dompet digital & perbankan.
4. Panduan Langkah demi Langkah Optimasi Baterai 5000mAh:
   - Aktifkan Mode Hemat Daya (Power Saving) di Pengaturan > Baterai jika baterai lemah.
   - Nyalakan "Pengisian Daya Malam Teroptimalkan" di Pengaturan > Baterai > Pengaturan Lainnya agar pengisian diperlambat di atas 80% saat tidur.
   - Matikan "Luncur Otomatis" (Auto-Launch) aplikasi di Manajer Telepon > Manajemen Aplikasi > Luncur Otomatis untuk menghentikan aplikasi sosmed/e-commerce mencuri RAM di belakang layar.
   - Gunakan fitur "Bekukan Cepat" (Quick App Freeze) di Pengaturan > Baterai > Manajemen Energi Aplikasi pada aplikasi yang jarang dibuka.
   - Kurangi waktu tunggu layar otomatis (Screen Timeout) ke 15 atau 30 detik di Pengaturan > Layar & Kecerahan.
5. Rekomendasi Aplikasi Baterai & Performa:
   - Manajer Telepon (bawaan Oppo): Pengoptimalan sistem 1-klik, pembersihan cache, perlindungan bayar.
   - Greenify: Untuk menidurkan paksa (hibernasi) aplikasi latar belakang berdaya tinggi.
   - AccuBattery: Memantau keawetan sel baterai asli dan kecepatan arus pengisian daya 10W.

Jawablah pertanyaan pengguna dengan sangat terstruktur, menggunakan poin-poin yang mudah dipahami, berikan empati, dan sapa mereka sebagai pengguna Oppo yang cerdas. Jaga agar tidak terlalu bertele-tele namun tetap informatif.
"""

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is empty or placeholder!")
            return@withContext "Kunci API Gemini belum dikonfigurasi. Silakan masukkan GEMINI_API_KEY Anda di panel Secrets Google AI Studio."
        }

        val requestUrl = "$BASE_URL?key=$apiKey"

        // Build the request JSON manually to avoid complex serialization issues
        val requestJson = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJsonString(prompt)}
                    }
                  ]
                }
              ],
              "systemInstruction": {
                "parts": [
                  {
                    "text": ${escapeJsonString(SYSTEM_INSTRUCTION)}
                  }
                ]
              },
              "generationConfig": {
                "temperature": 0.7,
                "topP": 0.95,
                "topK": 40
              }
            }
        """.trimIndent()

        val requestBody = requestJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Unsuccessful response from Gemini: Code ${response.code}, Body: $errBody")
                    
                    return@withContext if (response.code == 400 && errBody.contains("API key not valid")) {
                        "Kunci API Gemini Anda tidak valid. Silakan periksa kembali pengaturan GEMINI_API_KEY di AI Studio."
                    } else if (response.code == 429) {
                        "Batas kuota API terlampaui. Silakan tunggu beberapa saat sebelum mencoba lagi."
                    } else {
                        "Terjadi kesalahan koneksi ke server Gemini (Kode: ${response.code}). Silakan coba lagi."
                    }
                }

                val responseBodyStr = response.body?.string() ?: return@withContext "Respons kosong diterima dari server."
                return@withContext parseResponseJson(responseBodyStr)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network exception during Gemini call", e)
            return@withContext "Gagal terhubung ke internet. Pastikan perangkat Anda terkoneksi ke Wi-Fi atau data seluler yang stabil."
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during Gemini call", e)
            return@withContext "Terjadi kesalahan tidak terduga: ${e.localizedMessage}"
        }
    }

    private fun escapeJsonString(str: String): String {
        return Moshi.Builder().build().adapter(String::class.java).toJson(str)
    }

    private fun parseResponseJson(jsonStr: String): String {
        try {
            // Traverse the JSON manually to extract the response text using Moshi/generic Maps
            val adapter = moshi.adapter(Map::class.java)
            val root = adapter.fromJson(jsonStr) ?: return "Format respons tidak valid."
            
            val candidates = root["candidates"] as? List<*> ?: return "Respons tidak mengandung data kandidat."
            if (candidates.isEmpty()) return "Fasilitas filter keamanan memblokir respons."
            
            val firstCandidate = candidates[0] as? Map<*, *> ?: return "Format data kandidat tidak valid."
            val content = firstCandidate["content"] as? Map<*, *> ?: return "Tidak ada konten respons ditemukan."
            val parts = content["parts"] as? List<*> ?: return "Konten respons kosong."
            if (parts.isEmpty()) return "Konten respons kosong."
            
            val firstPart = parts[0] as? Map<*, *> ?: return "Format data teks tidak valid."
            return firstPart["text"] as? String ?: "Selesai mengolah data, namun tidak ada teks."
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response JSON", e)
            return "Gagal menguraikan jawaban dari server AI. Silakan coba lagi."
        }
    }
}

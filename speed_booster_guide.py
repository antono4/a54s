#!/usr/bin/env python3
"""
Speed Booster Guide Generator
Membuat panduan optimasi untuk OPPO A54s
"""

import os

# Panduan Lengkap Optimasi OPPO A54s
GUIDE = """
╔══════════════════════════════════════════════════════════════╗
║           🚀 SPEED BOOSTER - PANDUAN OPTIMASI 🚀            ║
║                    Untuk OPPO A54s                          ║
╚══════════════════════════════════════════════════════════════╝

📱 INFO PERANGKAT:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Model: OPPO A54s
• RAM: 4 GB
• Penyimpanan: 64 GB
• Android: 11
• ColorOS: 11
• Status Update: EOL (End of Life - Tidak ada update resmi)

⚡ CARA OPTIMASI CEPAT DI HP:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1️⃣  MATIKAN ANIMASI
    ━━━━━━━━━━━━━━━━━━
    • Buka Settings > About Phone
    • Ketuk "Version" 7 kali untukaktifkan Developer Options
    • Buka Settings > Additional Settings > Developer Options
    • Atur semua opsi animasi ke "Off" atau "0.5x"
    • Hasil: HP terasa lebih responsif dan cepat

2️⃣  BATASI APLIKASI BACKGROUND
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━
    • Buka Settings > Battery
    • Pilih "Power Saving Modes"
    • Aktifkan mode hemat daya
    • Atau: Settings > App Management > App List
    • Matikan "Allow Auto Launch" untuk aplikasi tidak penting

3️⃣  CLEAR CACHE REGULER
    ━━━━━━━━━━━━━━━━━━━━━
    • Buka Settings > Storage
    • Ketuk "Clean Up" atau "Free Up Space"
    • Hapus file cache secara berkala
    • Lakukan seminggu sekali

4️⃣  KELOLA RAM MANUAL
    ━━━━━━━━━━━━━━━━━━━━
    • Tekan tombol Recent Apps
    • Geser aplikasi ke kiri/kanan untuk menutup
    • Atau ketuk "Clear All"
    • Biarkan hanya aplikasi yang sedang digunakan

5️⃣  GUNAKAN VERSI LITE APLIKASI
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    • Facebook Lite
    • Instagram Lite  
    • Twitter Lite
    • YouTube Go
    • WhatsApp (bawaan sudah ringan)
    • Ini menghemat RAM dan penyimpanan

6️⃣  Pindahkan DATA KE SD CARD
    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    • Buka Settings > Storage
    • Pilih "SD Card" sebagai default
    • Pindahkan foto, video, musik ke SD card
    • Ini membebaskan memori internal

7️⃣  RESTART REGULER
    ━━━━━━━━━━━━━━━━━━
    • Restart HP 1-2 kali seminggu
    • Ini membersihkan RAM dan menyegarkan sistem

🔧 PENGATURAN LAINNYA:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✦ Kurangi Kecerahan Layar
  → Buka Settings > Display & Brightness
  → Aktifkan "Auto Brightness" atau turunkan manual

✦ Matikan Getaran yang Tidak Perlu
  → Settings > Sound & Vibration
  → Matikan haptic feedback yang tidak penting

✦ Nonaktifkan AOD (Always On Display)
  → Settings > Display & Brightness > AOD
  → Matikan jika tidak digunakan

✦ Kurangi Sinkronisasi Otomatis
  → Settings > Accounts
  → Matikan sync untuk aplikasi yang tidak perlu

✦ Hapus Widget yang Tidak Digunakan
  → Widget menggunakan RAM dan baterai

⚠️ JANGAN LAKUKAN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✗ Jangan install aplikasi pembersih RAM pihak ketiga
  → Kebanyakan tidak berguna dan justru memperlambat HP

✗ Jangan install custom ROM tanpa pengetahuan cukup
  → Bisa merusak HP dan menghilangkan garansi

✗ Jangan gunakan task killer berlebihan
  → Android dirancang untuk mengelola RAM sendiri

✗ Jangan matikan semua animasi (bisa menyebabkan glitch)

📊 JADWAL PERAWATAN:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

HARIAN:
  • Restart HP
  • Clear recent apps sebelum tidur

MINGGUAN:
  • Bersihkan cache (Settings > Storage > Clean Up)
  • Cek penggunaan data
  • Hapus aplikasi yang tidak digunakan

BULANAN:
  • Backup data penting
  • Format SD card (jika ada)
  • Cek kesehatan baterai

🆘 JIKA HP MASIH LAMBAT:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. Factory Reset (Last Resort)
   → Backup semua data dulu!
   → Settings > Additional Settings > Reset Options
   → "Erase All Data"

2.换手机 (Upgrade HP)
   → Jika sudah sangat lambat, pertimbangkan upgrade
   → HP dengan Android 13+ lebih optimal untuk apps modern

═══════════════════════════════════════════════════════════════
                    Dibuat dengan ❤️ untuk OPPO A54s
═══════════════════════════════════════════════════════════════
"""

def main():
    print(GUIDE)
    
    # Save to file
    with open('oppo_a54s_optimasi_guide.txt', 'w', encoding='utf-8') as f:
        f.write(GUIDE)
    
    print("\n📁 Panduan disimpan ke: oppo_a54s_optimasi_guide.txt")

if __name__ == '__main__':
    main()
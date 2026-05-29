package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.ChecklistItem
import com.example.db.UpdateRepository
import com.example.gemini.GeminiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    INSTALLING,
    REBOOTING,
    UPDATED_SUCCESS
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = UpdateRepository(database)

    // Expose data from database
    val checklistItems: StateFlow<List<ChecklistItem>> = repository.allChecklistItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val appLogs = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Local UI States
    private val _currentTab = MutableStateFlow("update")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadSpeed = MutableStateFlow("")
    val downloadSpeed: StateFlow<String> = _downloadSpeed.asStateFlow()

    private val _timeRemaining = MutableStateFlow("")
    val timeRemaining: StateFlow<String> = _timeRemaining.asStateFlow()

    private val _installProgress = MutableStateFlow(0f)
    val installProgress: StateFlow<Float> = _installProgress.asStateFlow()

    private val _showChecklistWarning = MutableStateFlow(false)
    val showChecklistWarning: StateFlow<Boolean> = _showChecklistWarning.asStateFlow()

    // Chat States
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            content = "Halo! Saya adalah Asisten Pendukung Oppo A54s. Ada yang bisa saya bantu terkait ColorOS, update, atau performa HP Anda?",
            isUser = false
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeChecklistIfNeeded()
            repository.addLog("Aplikasi Asisten Update Oppo A54s diluncurkan.", "INFO")
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun triggerCheckUpdate() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.CHECKING
            repository.addLog("Memulai pencarian pembaruan sistem Oppo A54s...", "INFO")
            delay(3000) // Simulate network delay
            _updateStatus.value = UpdateStatus.UPDATE_AVAILABLE
            repository.addLog("Pembaruan ditemukan! ColorOS 12.1 Berbasis Android 12 (CPH2273_11_C.18). Ukuran: 3.2 GB.", "SUCCESS")
        }
    }

    fun attemptDownload() {
        viewModelScope.launch {
            // Check if all checklist items are checked off
            val uncheckedCount = checklistItems.value.count { !it.isChecked }
            if (uncheckedCount > 0) {
                _showChecklistWarning.value = true
                repository.addLog("Peringatan: Mencoba update tanpa menyelesaikan semua persiapan ($uncheckedCount belum dicentang).", "WARNING")
            } else {
                startDownload()
            }
        }
    }

    fun dismissWarning() {
        _showChecklistWarning.value = false
    }

    fun forceStartDownload() {
        _showChecklistWarning.value = false
        startDownload()
    }

    private fun startDownload() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.DOWNLOADING
            repository.addLog("Mulai mengunduh berkas firmware CPH2273_11_C.18...", "INFO")
            
            val speeds = listOf("2.8 MB/s", "3.5 MB/s", "4.1 MB/s", "4.7 MB/s", "5.2 MB/s", "4.9 MB/s")
            
            for (i in 1..100) {
                delay(120) // Simulated speed
                val progress = i / 100f
                _downloadProgress.value = progress
                
                _downloadSpeed.value = speeds[i % speeds.size]
                val secRemaining = ((100 - i) * 0.4).toInt()
                _timeRemaining.value = if (secRemaining > 0) "$secRemaining detik tersisa" else "Hampir selesai"

                if (i == 25) {
                    repository.addLog("Mengunduh modul sistem ColorOS 12.1... (25%)", "INFO")
                } else if (i == 50) {
                    repository.addLog("Mengunduh patch keamanan Google & kustomisasi Oppo... (50%)", "INFO")
                } else if (i == 75) {
                    repository.addLog("Mengunduh modul rendering visual & RAM Expansion... (75%)", "INFO")
                } else if (i == 100) {
                    repository.addLog("Pengunduhan firmware selesai! Ukuran terunduh: 3.20 GB.", "SUCCESS")
                }
            }

            delay(1500)
            repository.addLog("Memulai verifikasi checksum SHA256 paket sistem...", "INFO")
            delay(2000)
            repository.addLog("Verifikasi checksum berhasil. Berkas firmware valid dan aman.", "SUCCESS")
            delay(1000)
            
            // Move to Installing inside ColorOS recovery screen
            startInstalling()
        }
    }

    private fun startInstalling() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.INSTALLING
            repository.addLog("Ponsel memasuki boot instalasi sistem otomatis...", "INFO")
            
            for (i in 1..100) {
                delay(150) // Installation process write
                _installProgress.value = i / 100f
                
                if (i == 10) {
                    repository.addLog("Memulai unzipping partisi OS...", "INFO")
                } else if (i == 30) {
                    repository.addLog("Menulis partisi sistem vendor (Android 12)...", "INFO")
                } else if (i == 60) {
                    repository.addLog("Memperbarui subsistem recovery & driver MediaTek Helio G35...", "INFO")
                } else if (i == 85) {
                    repository.addLog("Membangun ulang struktur cache & mengoptimalkan aplikasi terpasang...", "INFO")
                } else if (i == 100) {
                    repository.addLog("Penulisan partisi firmware selesai!", "SUCCESS")
                }
            }

            delay(2000)
            repository.addLog("Memulai muat ulang perangkat (Reboot)...", "WARNING")
            _updateStatus.value = UpdateStatus.REBOOTING
            
            delay(4000) // Simulation of screen blank and rebooting
            _updateStatus.value = UpdateStatus.UPDATED_SUCCESS
            repository.addLog("Perangkat berhasil diupdate ke ColorOS 12.1 / Android 12 (CPH2273_11_C.18)!", "SUCCESS")
            repository.addLog("Semua sistem berjalan normal. RAM Expansion siap digunakan.", "SUCCESS")
        }
    }

    fun addLog(message: String, type: String = "INFO") {
        viewModelScope.launch {
            repository.addLog(message, type)
        }
    }

    fun toggleCheckItem(item: ChecklistItem) {
        viewModelScope.launch {
            val updated = item.copy(isChecked = !item.isChecked)
            repository.updateChecklistItem(updated)
            val logMessage = if (updated.isChecked) {
                "Selesai mempersiapkan: ${updated.title}"
            } else {
                "Membatalkan persiapan: ${updated.title}"
            }
            repository.addLog(logMessage, "INFO")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.addLog("Riwayat aktivitas dibersihkan.", "INFO")
        }
    }

    fun resetUpdateState() {
        _updateStatus.value = UpdateStatus.IDLE
        _downloadProgress.value = 0f
        _installProgress.value = 0f
        _downloadSpeed.value = ""
        _timeRemaining.value = ""
        viewModelScope.launch {
            repository.addLog("Mereset simulator pembaruan ke mode default.", "INFO")
        }
    }

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return

        val userMsg = ChatMessage(content = text, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            repository.addLog("Bertanya pada asisten AI: '$text'", "INFO")
            val reply = GeminiService.generateResponse(text)
            _chatMessages.value = _chatMessages.value + ChatMessage(content = reply, isUser = false)
            _isChatLoading.value = false
        }
    }
}

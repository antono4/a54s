package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.ChecklistItem
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(viewportViewModel: UpdateViewModel = viewModel()) {
    val currentTab by viewportViewModel.currentTab.collectAsState()
    val updateStatus by viewportViewModel.updateStatus.collectAsState()

    // If we are in INSTALLING or REBOOTING, we override the normal scaffold with full-screen recovery overlays!
    if (updateStatus == UpdateStatus.INSTALLING || updateStatus == UpdateStatus.REBOOTING) {
        RecoveryScreen(viewportViewModel)
    } else {
        Scaffold(
            bottomBar = {
                AppBottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewportViewModel.setTab(it) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentTab, label = "tabTransition") { tab ->
                    when (tab) {
                        "update" -> UpdateTabScreen(viewportViewModel)
                        "specs" -> SpecsTabScreen(viewportViewModel)
                        "checklist" -> ChecklistTabScreen(viewportViewModel)
                        "chat" -> ChatTabScreen(viewportViewModel)
                    }
                }
            }
        }
    }
}

// --- Dynamic Bottom Navigation Bar ---
@Composable
fun AppBottomNavigationBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("app_bottom_nav_bar")
    ) {
        NavigationBarItem(
            selected = currentTab == "update",
            onClick = { onTabSelected("update") },
            label = { Text("Pembaruan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Tab Pembaruan") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == "specs",
            onClick = { onTabSelected("specs") },
            label = { Text("Spesifikasi", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Info, contentDescription = "Tab Spesifikasi") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == "checklist",
            onClick = { onTabSelected("checklist") },
            label = { Text("Persiapan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Tab Persiapan") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        NavigationBarItem(
            selected = currentTab == "chat",
            onClick = { onTabSelected("chat") },
            label = { Text("AI Asisten", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Tab Asisten") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

// ==========================================
// 1. PEMBARUAN (UPDATE STATUS & SIMULATION) TAB
// ==========================================
@Composable
fun UpdateTabScreen(viewModel: UpdateViewModel) {
    val updateStatus by viewModel.updateStatus.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadSpeed by viewModel.downloadSpeed.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val showWarning by viewModel.showChecklistWarning.collectAsState()
    val appLogs by viewModel.appLogs.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Auto scroll logs when new items are added
    LaunchedEffect(appLogs.size) {
        if (appLogs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (showWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWarning() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Persiapan Belum Lengkap", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Anda belum mencentang semua item pada daftar persiapan update. " +
                    "Melakukan pembaruan sistem tanpa persiapan (seperti cadangan data/baterai cukup) " +
                    "sangat berisiko menyebabkan crash sistem atau kehilangan data.\n\n" +
                    "Apakah Anda yakin ingin tetap melanjutkan simulasi ini?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.forceStartDownload() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Tetap Lanjutkan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.dismissWarning()
                        viewModel.setTab("checklist")
                    }
                ) {
                    Text("Selesaikan Persiapan", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner (Bento style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CENTRAL UPDATE COLOROS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Update Companion & Simulator Oppo A54s",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Kelola, pantau, dan simulasikan pembaruan sistem perangkat Anda dengan cerdas dan aman.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Current Hardware Status Widget (Bento Grid cells)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Status Perangkat Saat Ini",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cell 1: Model
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Model Perangkat", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("OPPO A54s", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("CPH2273", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                    
                    // Cell 2: ColorOS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("ColorOS", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("11.1", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Versi Asli", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cell 3: Android
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Android", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("11", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Versi Dasar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                    
                    // Cell 4: Security
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Keamanan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("1 Agt 2021", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Patch Stabil", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        // Animated Simulation Card (Bento Style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (updateStatus) {
                        UpdateStatus.IDLE -> {
                            Text(
                                "Pemeriksaan Pembaruan Sistem",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Bento Glowing Checkmark
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Device Ready",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Semua sistem saat ini terindikasi berjalan lancar.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = { viewModel.triggerCheckUpdate() },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("check_update_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Periksa")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cari Pembaruan Terbaru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
 
                        UpdateStatus.CHECKING -> {
                            Text(
                                "Menghubungkan ke Server Oppo...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            
                            // Radar Pulse Animation
                            RadarPulsingAnimation()
                            
                            Spacer(modifier = Modifier.height(28.dp))
                            Text(
                                "Mencocokkan tanda firmware untuk model CPH2273...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
 
                        UpdateStatus.UPDATE_AVAILABLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Cell A: Hero New Update Release
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "VERSI TERBARU TERSEDIA",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                letterSpacing = 1.sp
                                            )
                                            // Live point animation
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                                    Text("Siap Unduh", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "ColorOS 12.1",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 26.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Android 12 | CPH2273_11_C.18",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Cell B: Size Card (Bento grid style)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Ukuran File", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("3.20 GB", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                    
                                    // Cell C: Android version Card (Bento grid style)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text("Keamanan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Aman & Stabil", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
 
                                // Cell D: Changelog (Bento grid style)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Apa yang baru?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Icon(Icons.Default.ArrowForward, contentDescription = "Detail", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "• Desain Visual Baru 'Aesthetic Space' & Fluiditas Elok.\n" +
                                            "• Fitur Ekspansi RAM Tambahan Virtual hingga +3 GB murni.\n" +
                                            "• Fitur Peningkatan Proteksi Privasi & Indikator Keamanan.\n" +
                                            "• System Booster 30% meminimalisir frame drop pada Helio G35.",
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                        )
                                    }
                                }
 
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetUpdateState() },
                                        shape = RoundedCornerShape(24.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Batal")
                                    }
                                    Button(
                                        onClick = { viewModel.attemptDownload() },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(2f).height(48.dp).testTag("download_update_button")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Unduh")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unduh & Pasang", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
 
                        UpdateStatus.DOWNLOADING -> {
                            Text("Mengunduh Dan Mempersiapkan Paket Update", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val progressPercent = (downloadProgress * 100).toInt()
                            
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                                CircularProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    strokeCap = StrokeCap.Round,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$progressPercent%", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                    Text("Terunduh", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Kecepatan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(downloadSpeed, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Estimasi Waktu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(timeRemaining, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        }
 
                        UpdateStatus.UPDATED_SUCCESS -> {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Update Match Complete",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pembaruan Sistem Berhasil!", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Perangkat OPPO A54s Anda kini berjalan di firmware resmi terbaru:",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SISTEM SEKARANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("ColorOS 12.1 • Android 12", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("CPH2273_11_C.18", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.resetUpdateState() },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Reset Simulator Pembaruan", fontWeight = FontWeight.Bold)
                            }
                        }
 
                        else -> {}
                    }
                }
            }
        }

        // Realtime Application Logs Consoles Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070B09)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1B2F25)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Konsol Aktivitas Pembaruan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear logs", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (appLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Konsol kosong. Mulai aktivitas untuk melihat log.", color = Color.DarkGray, fontSize = 11.sp)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(appLogs) { log ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    val logColor = when (log.type) {
                                        "SUCCESS" -> Color(0xFF39FF14) // Neon green
                                        "WARNING" -> Color(0xFFFFA500) // Orange
                                        "ERROR" -> Color(0xFFFF3333) // Red
                                        else -> Color(0xFF8CD3C7) // Tealish white
                                    }
                                    Text(
                                        text = "[${log.type}] ${log.message}",
                                        color = logColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Radar Pulse Effect Composable
@Composable
fun RadarPulsingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radiusRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val opacityRatio by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacity"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 2

            // Glowing Animated Pulse Wave Left-Right
            drawCircle(
                color = primaryColor,
                radius = baseRadius * radiusRatio,
                alpha = opacityRatio,
                style = Stroke(width = 3.dp.toPx())
            )

            // Dynamic Inner Circle
            drawCircle(
                color = primaryColor,
                radius = 16.dp.toPx(),
                alpha = 0.8f
            )
        }
        Icon(
            Icons.Default.Settings,
            contentDescription = "Gear",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ==========================================
// 2. SPESIFIKASI & MEMORY CLEANER TAB
// ==========================================
@Composable
fun SpecsTabScreen(viewModel: UpdateViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // RAM expansion local states
    var ramExpansionValue by remember { mutableStateOf(3) } // None(0), +1GB(1), +2GB(2), +3GB(3)
    var isCleaning by remember { mutableStateOf(false) }
    var cleanResultProgress by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Detail Spesifikasi OPPO A54s", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Text("Kenali mesin internal perangkat Anda untuk penyesuaian stabilitas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }

        // Active Memory Optimizer Card (Bento Style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Manajer RAM & Storage", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Optimasi satu ketukan untuk performa lancar.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Icon(Icons.Default.Memory, contentDescription = "Processor", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Penyimpanan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                Text("128 GB", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Longgar & Luas", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("RAM Fisik", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                Text("4 GB", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("LPDDR4x", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Pembersih Memori Singgahan (Cache)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isCleaning) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Memindai file sampah...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${(cleanResultProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { cleanResultProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isCleaning = true
                                cleanResultProgress = 0f
                                coroutineScope.launch {
                                    viewModel.addLog("Mengeksekusi One-Tap RAM & Cache Cleanup...", "INFO")
                                    while (cleanResultProgress < 1f) {
                                        delay(150)
                                        cleanResultProgress += 0.1f
                                    }
                                    isCleaning = false
                                    viewModel.addLog("Berhasil membebaskan 342 MB RAM dan menghapus sampah cache.", "SUCCESS")
                                    Toast.makeText(context, "Berhasil membebaskan 342 MB dari cache sistem!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = "Bersihkan Cache")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bersihkan Cache Dan Legakan Memori", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Oppo Virtual RAM Expansion Simulator Widget (Bento Style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.OfflineBolt, contentDescription = "RAM Expand icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Teknologi Ekspansi RAM (Virtual RAM)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Ubah kapasitas penyimpanan internal (ROM) yang tidak terpakai menjadi memori virtual transisi. " +
                        "Dukungan RAM virtual Oppo pada ColorOS 12.1 mampu menambahkan RAM hingga +3GB.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Virtual Slider Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2, 3).forEach { ramVal ->
                            val isSelected = ramExpansionValue == ramVal
                            val textLabel = if (ramVal == 0) "Mati" else "+${ramVal} GB"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        ramExpansionValue = ramVal
                                        coroutineScope.launch {
                                            val logMsg = if (ramVal == 0) "Menonaktifkan fitur Ekspansi RAM otomatis." else "Mengaktifkan ekspansi RAM ke +$ramVal GB."
                                            viewModel.addLog(logMsg, "INFO")
                                        }
                                    }
                            ) {
                                Text(
                                    textLabel,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed Alert", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val netRam = 4 + ramExpansionValue
                            Text(
                                text = "Kalkulasi RAM Aktif: 4 GB + $ramExpansionValue GB Virtual = $netRam GB RAM Efektif!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Expanded Specs Grid Panel (Bento Style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Lembar Spesifikasi Teknis OPPO A54s", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    SpecRowItem("Chipset", "MediaTek Helio G35 (12nm)")
                    SpecRowItem("Arsitektur CPU", "Octa-core (4x2.3 GHz Cortex-A53 + 4x1.8 GHz)")
                    SpecRowItem("Kartu Grafis", "PowerVR GE8320 @680 MHz")
                    SpecRowItem("Tipe RAM", "4GB LPDDR4x Single Channel")
                    SpecRowItem("Penyimpanan", "128GB eMMC 5.1 (MicroSD up to 256GB)")
                    SpecRowItem("Layar", "6.52 inci IPS LCD, HD+ (720x1600 px), 60Hz")
                    SpecRowItem("Baterai", "5000 mAh Li-Po, Pengisian daya 10W")
                    SpecRowItem("Kamera Utama", "50 MP (f/1.8) Utama + 2 MP Macro + 2 MP Depth")
                    SpecRowItem("Sistem Asli", "ColorOS 11.1 Berbasis Android 11")
                }
            }
        }
    }
}

@Composable
fun SpecRowItem(title: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.widthIn(max = 200.dp))
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

// ==========================================
// 3. DAFTAR PERSIAPAN (CHECKLIST) & OPTIMASI TAB
// ==========================================
@Composable
fun ChecklistTabScreen(viewModel: UpdateViewModel) {
    val checklistItems by viewModel.checklistItems.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-tab selection state inside checklist page
    var activeSubTab by remember { mutableStateOf("update") } // "update", "privacy", "battery"

    // Battery Optimizer States
    var isOptimizingBattery by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(false) }
    var batteryOptimizationProgress by remember { mutableStateOf(0f) }
    var simulatedStandbyHours by remember { mutableStateOf(14.3f) } // 14.3 hours initially

    // Security Audit States
    var isSecurityChecked by remember { mutableStateOf(false) }
    var isAuditingSecurity by remember { mutableStateOf(false) }
    var auditProgress by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title & Context Header
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Optimalisasi & Pengaturan",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Konfigurasikan sistem, privasi data, dan keawetan baterai OPPO A54s Anda.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Sub-Tabs Capsule Bar (Bento style)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "update" to "⚙️ Update",
                        "privacy" to "🔒 Privasi",
                        "battery" to "🔋 Baterai"
                    )
                    tabs.forEach { (key, label) ->
                        val isSelected = activeSubTab == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeSubTab = key }
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        // Interactive Component Based on Selected Sub-Tab
        when (activeSubTab) {
            "battery" -> {
                // Interactive Battery Simulator Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Simulasi Smart Battery Saver", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Perpanjang masa aktif sel baterai Oppo 5000mAh", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery Status", tint = if (isBatteryOptimized) Color(0xFF00A26C) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Battery Standby Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Suhu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text(if (isBatteryOptimized) "31.4°C" else "36.8°C", fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isBatteryOptimized) Color(0xFF00A26C) else MaterialTheme.colorScheme.onSurface)
                                    Text(if (isBatteryOptimized) "Dingin & Optimal" else "Sedikit Hangat", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                
                                Divider(modifier = Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val hours = simulatedStandbyHours.toInt()
                                    val minutes = ((simulatedStandbyHours - hours) * 60).toInt()
                                    Text("Estimasi Siaga", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("$hours Jam $minutes Menit", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(if (isBatteryOptimized) "Performa Sangat Awet" else "Konsumsi Standar", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }

                                Divider(modifier = Modifier.height(36.dp).width(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Kesehatan Sel", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("92%", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF00A26C))
                                    Text("Kondisi Prima", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isOptimizingBattery) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val currentProgressPercent = (batteryOptimizationProgress * 100).toInt()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when {
                                                batteryOptimizationProgress < 0.3f -> "Menghentikan proses background Helios G35..."
                                                batteryOptimizationProgress < 0.6f -> "Membekukan aktivitas konsumsi daya tinggi..."
                                                batteryOptimizationProgress < 0.9f -> "Menyesuaikan tidur cerdas sensor IPS LCD..."
                                                else -> "Finishing optimalisasi baterai..."
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("$currentProgressPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { batteryOptimizationProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isOptimizingBattery = true
                                        batteryOptimizationProgress = 0f
                                        coroutineScope.launch {
                                            viewModel.addLog("Memulai uji optimalisasi baterai OPPO A54s...", "INFO")
                                            delay(500)
                                            while (batteryOptimizationProgress < 1f) {
                                                delay(120)
                                                batteryOptimizationProgress += 0.08f
                                            }
                                            isOptimizingBattery = false
                                            isBatteryOptimized = true
                                            simulatedStandbyHours = 22.8f // Jumps from 14.3h to 22.8h (+8.5 hours standby!)
                                            viewModel.addLog("Optimalisasi baterai selesai! Tambahan sisa waktu standby +8 Jam 30 Menit diaktifkan.", "SUCCESS")
                                            viewModel.addLog("Aktivitas sinkronisasi latar belakang sosial dibekukan sementara di ColorOS.", "SUCCESS")
                                            Toast.makeText(context, "Baterai berhasil dioptimalkan! Mode Hemat Daya Aktif.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBatteryOptimized) Color(0xFF00A26C) else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("battery_optimize_btn")
                                ) {
                                    Icon(
                                        if (isBatteryOptimized) Icons.Default.Verified else Icons.Default.Bolt,
                                        contentDescription = "Optimize Button"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isBatteryOptimized) "Sistem Baterai Dioptimalkan!" else "Optimalkan Daya Sekarang",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // App Recommendations Bento Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Aplikasi Pendukung & Penghemat Daya",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val appRecommendations = listOf(
                                Triple("Oppo Phone Manager", "Aplikasi bawaan orisinal Oppo. Menawarkan pembersihan virus, monitor RAM, dan pengoptimalan 1-ketukan secara instan dan aman.", Icons.Default.Security),
                                Triple("Greenify", "Sangat efektif untuk menghibernasi aplikasi berat (sosmed/game) di belakang layar agar tidak menyedot daya Helio G35 terus-menerus.", Icons.Default.Air),
                                Triple("AccuBattery", "Menganalisis kapasitas asli sel baterai 5000mAh Anda, merekam kecepatan charge, dan merekomendasikan alarm pengisian 80% demi menjaga kesehatan baterai.", Icons.Default.QueryStats)
                            )
                            
                            appRecommendations.forEachIndexed { idx, (appName, desc, icon) ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = appName, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(appName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(desc, fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            "privacy" -> {
                // Security Audit Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Ulasan Proteksi Keamanan & Privasi", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Periksa celah kerentanan & perlindungan data HP Anda", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.VerifiedUser, contentDescription = "Security Audit", tint = if (isSecurityChecked) Color(0xFF00A26C) else MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Patch Saat Ini", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("1 Agustus 2021", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Versi Bawaan", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Patch Tersedia", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text("Juli 2022", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Di ColorOS 12.1", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Keamanan Privasi", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                    Text(if (isSecurityChecked) "Terpoteksi Baik" else "Butuh Tinjauan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSecurityChecked) Color(0xFF00A26C) else MaterialTheme.colorScheme.tertiary)
                                    Text("Enkripsi 256-bit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (isAuditingSecurity) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    val auditPercent = (auditProgress * 100).toInt()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = when {
                                                auditProgress < 0.4f -> "Memindai integritas file sistem Oppo..."
                                                auditProgress < 0.7f -> "Memeriksa izin izin sensor mencurigakan..."
                                                else -> "Memvalidasi sertifikasi Google Play Protect..."
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("$auditPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { auditProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        isAuditingSecurity = true
                                        auditProgress = 0f
                                        coroutineScope.launch {
                                            viewModel.addLog("Memulai Audit Keamanan HP OPPO A54s...", "INFO")
                                            delay(400)
                                            while (auditProgress < 1f) {
                                                delay(150)
                                                auditProgress += 0.1f
                                            }
                                            isAuditingSecurity = false
                                            isSecurityChecked = true
                                            viewModel.addLog("Audit keamanan selesai! Sistem terbukti aman tetapi patch 2021 usang.", "WARNING")
                                            viewModel.addLog("Sangat dianjurkan memperbarui ke ColorOS 12.1 untuk mendapatkan patch keamanan Juli 2022.", "SUCCESS")
                                            Toast.makeText(context, "Audit Selesai! Terapkan langkah keamanan di checklist di bawah ini.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSecurityChecked) Color(0xFF00A26C) else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("security_audit_btn")
                                ) {
                                    Icon(
                                        if (isSecurityChecked) Icons.Default.GppGood else Icons.Default.Fingerprint,
                                        contentDescription = "Scan Button"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isSecurityChecked) "Keamanan Sukses Ditinjau!" else "Audit & Pindai Keamanan",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subcategory Title For Items List
        item {
            Text(
                text = when (activeSubTab) {
                    "battery" -> "Panduan Langkah demi Langkah Hemat Daya"
                    "privacy" -> "Rekomendasi Setelan Privasi Data"
                    else -> "Langkah Persiapan Sistem Update"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp, top = 4.dp)
            )
        }

        // Filter and display the checklists depending on the selected sub-tab
        val filteredList = when (activeSubTab) {
            "battery" -> checklistItems.filter { it.id in 20..29 }
            "privacy" -> checklistItems.filter { it.id in 10..19 }
            else -> checklistItems.filter { it.id in 1..9 }
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Sedang memuat data panduan...",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(filteredList) { item ->
                CheckItemWidget(
                    item = item,
                    onToggle = { viewModel.toggleCheckItem(item) }
                )
            }
        }

        // Tips Card based on sub tab
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = "Tips Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (activeSubTab) {
                                "battery" -> "Tips Ekstra: Kecerahan Otomatis"
                                "privacy" -> "Tips Ekstra: Enkripsi Sandi"
                                else -> "Info Penting: Masa Support Update"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (activeSubTab) {
                                "battery" -> "Penggunaan kecerahan layar manual di atas 70% pada layar IPS LCD sangat mempercepat pengosongan baterai. Mengaktifkan 'Kecerahan Otomatis' dapat menghemat hingga 20% daya harian."
                                "privacy" -> "Jangan pernah menyamakan kata sandi layar kunci luar dengan kata sandi Brankas Pribadi (Private Safe) atau Kunci Aplikasi (App Lock), guna mengantisipasi pencurian data saat ponsel terpinjam."
                                else -> "Oppo A54s secara resmi hanya mendapatkan update stabil hingga ColorOS 12.1 (Android 12). Oppo memutuskan untuk tidak mengizinkan pembaruan ke ColorOS 13 demi stabilitas performa prosesor Helio G35 Anda agar terhindar dari lag kronis."
                            },
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckItemWidget(
    item: ChecklistItem,
    onToggle: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (item.isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("checklist_card_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==========================================
// 4. CHAT ASISTEN AI (GEMINI ENGINE) TAB
// ==========================================
@Composable
fun ChatTabScreen(viewModel: UpdateViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // Auto scroll chat to the bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = "Bot", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Dukungan Cerdas OPPO AI", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Green, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Asisten Virtual Siap Membantu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

        // Chat messages body list
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada pesan. Mulailah mengetik tanyakan sesuatu!", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubbleWidget(msg)
                    }

                    if (isChatLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("AI sedang mengetik...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Suggested prompts deck helper
        FlowPromptSuggestionRow(
            onPromptClick = { prompt ->
                inputText = ""
                viewModel.sendChatMessage(prompt)
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Input send box
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Tanyakan performa, update, lag, baterai...", fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .testTag("chat_text_input")
            )

            FloatingActionButton(
                onClick = {
                    if (inputText.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp).testTag("chat_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubbleWidget(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val containerColor = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }

        val textColor = if (isUser) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onBackground
        }

        val roundedCorners = if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
        } else {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = roundedCorners,
            modifier = Modifier
                .widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun FlowPromptSuggestionRow(
    onPromptClick: (String) -> Unit
) {
    val prompts = listOf(
        "Bagaimana aktifkan RAM tambahan Oppo?",
        "Mengapa HP Oppo A54s saya lambat?",
        "Apakah bisa update ke Android 13/14?"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Rekomendasi Pertanyaan:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            prompts.forEach { p ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable { onPromptClick(p) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        p,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. BOOT/RECOVERY SIMULATION OVERLAYS
// ==========================================
@Composable
fun RecoveryScreen(viewModel: UpdateViewModel) {
    val updateStatus by viewModel.updateStatus.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()

    if (updateStatus == UpdateStatus.INSTALLING) {
        Surface(
            color = Color(0xFF020403), // Jet black recovery screen
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Oppo Logo text simulated elegantly
                Text(
                    "O P P O",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 32.sp,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "ColorOS Recovery MODE",
                    color = Color(0xFF00A26C),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(54.dp))

                // Custom Circular Installation Ring
                val progressPercent = (installProgress * 100).toInt()
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                    CircularProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFF00A26C),
                        trackColor = Color(0xFF132F23),
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$progressPercent%", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Memasang", fontSize = 10.sp, color = Color.Gray, letterSpacing = 2.sp)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    "Instalasi Pembaruan Sistem...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Harap jangan mematikan atau mengoperasikan ponsel Anda.\nPonsel akan otomatis dimulai ulang setelah instalasi selesai.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                
                // Progress Bar Secondary horizontal lines
                LinearProgressIndicator(
                    progress = { installProgress },
                    color = Color(0xFF00A26C),
                    trackColor = Color(0xFF132F23),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(CircleShape)
                )
            }
        }
    } else if (updateStatus == UpdateStatus.REBOOTING) {
        // Reboot Blank transition screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Simulated Fade-In Oppo Logo blinking
            val animatedAlpha by rememberInfiniteTransition(label = "rebootFade").animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "OPPO",
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = animatedAlpha),
                    fontSize = 36.sp,
                    letterSpacing = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Powered by Android",
                    color = Color.Gray.copy(alpha = animatedAlpha),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

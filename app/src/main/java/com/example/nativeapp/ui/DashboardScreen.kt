package com.example.nativeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.utils.UpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    drafts: List<com.example.model.CVData>,
    onCreateNew: () -> Unit,
    onOpenDraft: (String) -> Unit,
    onDeleteDraft: (com.example.model.CVData) -> Unit,
    onGenerateDummy: () -> Unit,
    onImportProject: () -> Unit,
    onExportDraft: (com.example.model.CVData) -> Unit
) {
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI CV Maker", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Tentang Aplikasi")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Buat CV Baru")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hero Section (Gradient Banner)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Susun CV Profesional",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Berkelas rekruter internasional dalam hitungan menit dengan bantuan AI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onGenerateDummy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Muat Contoh CV", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onImportProject,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Impor (.cvm)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "Draf CV Anda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (drafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada CV.\nTekan + untuk membuat baru.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drafts, key = { it.id }) { draft ->
                        CvDraftCard(
                            draft = draft,
                            onClick = { onOpenDraft(draft.id) },
                            onDelete = { onDeleteDraft(draft) },
                            onExport = { onExportDraft(draft) }
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var updateMessage by remember { mutableStateOf<String?>(null) }
        val updateManager = remember { UpdateManager(context) }

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text("Tentang AI CV Maker", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Version 1.0.0", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Aplikasi pembuatan CV cerdas berbasis Artificial Intelligence. Tingkatkan peluang karir Anda dengan mudah.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Developer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text("Syahril Azhar Ramdhanu", fontWeight = FontWeight.Medium)
                    Text("aril.syahril149@gmail.com", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                        uriHandler.openUri("mailto:aril.syahril149@gmail.com")
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("© 2026 Syahril Azhar Ramdhanu. All rights reserved.\nPowered by Google Gemini AI.", fontSize = 10.sp, color = Color.Gray)
                    
                    if (updateMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(updateMessage!!, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isCheckingUpdate) return@TextButton
                        isCheckingUpdate = true
                        updateMessage = "Mengecek pembaruan..."
                        
                        coroutineScope.launch {
                            val release = updateManager.checkForUpdates("v1.0.0")
                            isCheckingUpdate = false
                            if (release != null) {
                                updateMessage = "Pembaruan ditemukan! Mengunduh..."
                                updateManager.downloadAndInstallUpdate(release)
                            } else {
                                updateMessage = "Anda menggunakan versi terbaru."
                            }
                        }
                    },
                    enabled = !isCheckingUpdate
                ) {
                    Text(if (isCheckingUpdate) "Mengecek..." else "Cek Pembaruan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun CvDraftCard(draft: com.example.model.CVData, onClick: () -> Unit, onDelete: () -> Unit, onExport: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(draft.lastModified))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Terakhir diubah: $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onExport,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Download,
                        contentDescription = "Ekspor CV",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Hapus CV",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

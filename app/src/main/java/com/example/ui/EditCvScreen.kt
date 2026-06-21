package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.utils.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCvScreen(
    viewModel: CvViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val draftState by viewModel.currentDraft.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isAiPolishing by viewModel.isAiPolishing.collectAsStateWithLifecycle()
    val aiPolishedResult by viewModel.aiPolishedResult.collectAsStateWithLifecycle()
    val originalText by viewModel.originalTextToPolish.collectAsStateWithLifecycle()
    val polishType by viewModel.polishFieldType.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) }

    // Callback used when acceptance of AI polish occurs
    var pendingApplyText by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val tabs = listOf(
        Pair("Profil", Icons.Filled.Person),
        Pair("Pengalaman", Icons.Filled.Work),
        Pair("Pendidikan", Icons.Filled.School),
        Pair("Keahlian", Icons.Filled.AutoAwesome),
        Pair("Ekspor", Icons.Filled.Download)
    )

    val draft = draftState

    // SAF File Creator
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && draft != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    PdfGenerator.generatePdf(draft, "#6366F1", os)
                }
                Toast.makeText(context, "CV Berhasil diekspor!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal mengekspor PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (isLoading || draft == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = draft.draftTitle,
                            onValueChange = { viewModel.updateDraftTitle(it) },
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("draft_title_input")
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.saveDraft(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveDraft()
                            Toast.makeText(context, "Draft disimpan!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabContentTransition",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        0 -> PersonalInfoTab(
                            personalInfo = draft.personalInfo,
                            onUpdate = { viewModel.updatePersonalInfo(it) },
                            onPolishSummary = { text, applyFn ->
                                pendingApplyText = applyFn
                                viewModel.triggerAiPolish("summary", text)
                            }
                        )
                        1 -> ExperiencesTab(
                            experiences = draft.experiences,
                            onAdd = { viewModel.addExperience(it) },
                            onUpdate = { viewModel.updateExperience(it) },
                            onDelete = { viewModel.removeExperience(it) },
                            onPolishExperience = { text, applyFn ->
                                pendingApplyText = applyFn
                                viewModel.triggerAiPolish("experience", text)
                            }
                        )
                        2 -> EducationTab(
                            educations = draft.educations,
                            onAdd = { viewModel.addEducation(it) },
                            onUpdate = { viewModel.updateEducation(it) },
                            onDelete = { viewModel.removeEducation(it) },
                            onPolishEducation = { text, applyFn ->
                                pendingApplyText = applyFn
                                viewModel.triggerAiPolish("education", text)
                            }
                        )
                        3 -> SkillsLanguagesTab(
                            skills = draft.skills,
                            languages = draft.languages,
                            onAddSkill = { viewModel.addSkill(it) },
                            onRemoveSkill = { viewModel.removeSkill(it) },
                            onAddLanguage = { viewModel.addLanguage(it) },
                            onRemoveLanguage = { viewModel.removeLanguage(it) }
                        )
                        4 -> ExportTab(
                            draft = draft,
                            viewModel = viewModel,
                            onSelectTemplate = { viewModel.updateTemplate(it.name) },
                            onExportPdf = {
                                val cleanTitle = draft.draftTitle.replace("\\s+".toRegex(), "_")
                                createDocumentLauncher.launch("$cleanTitle.pdf")
                            }
                        )
                    }
                }
            }

            // AI Polishing Loader overlay
            if (isAiPolishing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFEAB308))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("✨ AI sedang memoles kata-kata...", fontWeight = FontWeight.Bold)
                            Text(
                                "Menerjemahkan ke bahasa profesional terstruktur...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // AI Comparison Dialog
            if (aiPolishedResult != null) {
                val polishedText = aiPolishedResult!!
                val isError = polishedText.startsWith("Error:") || polishedText.startsWith("Gagal")

                AlertDialog(
                    onDismissRequest = { viewModel.clearAiPolish() },
                    confirmButton = {
                        if (!isError) {
                            Button(
                                onClick = {
                                    pendingApplyText?.invoke(polishedText)
                                    viewModel.clearAiPolish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308), contentColor = Color.White)
                            ) {
                                Text("Gunakan Hasil AI")
                            }
                        } else {
                            Button(onClick = { viewModel.clearAiPolish() }) {
                                Text("Oke")
                            }
                        }
                    },
                    dismissButton = {
                        if (!isError) {
                            TextButton(onClick = { viewModel.clearAiPolish() }) {
                                Text("Kembali")
                            }
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFEAB308))
                            Text("Pemolesan Profesional")
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isError) {
                                Text(polishedText, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text(
                                    "AI berhasil memoles deskripsi Anda agar terdengar lebih profesional dan berbobot bagi rekruter.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Column {
                                    Text("Kata Asli Anda:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(12.dp)
                                    ) {
                                        Text(originalText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Column {
                                    Text("Hasil Pemolesan Profesional AI: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFCA8A04))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFEAB308), RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFEF08A).copy(alpha = 0.2f))
                                            .padding(12.dp)
                                    ) {
                                        Text(polishedText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

// --- TAB 0: PERSONAL INFO ---
@Composable
fun PersonalInfoTab(
    personalInfo: PersonalInfo,
    onUpdate: (PersonalInfo) -> Unit,
    onPolishSummary: (String, (String) -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Informasi Kontak Pribadi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = personalInfo.fullName,
            onValueChange = { onUpdate(personalInfo.copy(fullName = it)) },
            label = { Text("Nama Lengkap") },
            placeholder = { Text("e.g. Budi Hartono") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("personal_name_input")
        )

        OutlinedTextField(
            value = personalInfo.title,
            onValueChange = { onUpdate(personalInfo.copy(title = it)) },
            label = { Text("Gelar Profesional / Jabatan") },
            placeholder = { Text("e.g. Senior Android Engineer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("personal_title_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = personalInfo.email,
                onValueChange = { onUpdate(personalInfo.copy(email = it)) },
                label = { Text("Email") },
                placeholder = { Text("e.g. budish@email.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = personalInfo.phone,
                onValueChange = { onUpdate(personalInfo.copy(phone = it)) },
                label = { Text("Telepon") },
                placeholder = { Text("e.g. +62812...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = personalInfo.location,
                onValueChange = { onUpdate(personalInfo.copy(location = it)) },
                label = { Text("Lokasi") },
                placeholder = { Text("e.g. Jakarta, ID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = personalInfo.website,
                onValueChange = { onUpdate(personalInfo.copy(website = it)) },
                label = { Text("Link Profil (LinkedIn/Github)") },
                placeholder = { Text("e.g. linkedin.com/in/budi") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tentang Saya / Ringkasan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Ketikan ringkasan singkat diri Anda", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = {
                    onPolishSummary(personalInfo.summary) { polished ->
                        onUpdate(personalInfo.copy(summary = polished))
                    }
                },
                modifier = Modifier
                    .background(Color(0xFFFEF08A), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "Polish dengan AI", tint = Color(0xFFCA8A04), modifier = Modifier.size(18.dp))
            }
        }

        OutlinedTextField(
            value = personalInfo.summary,
            onValueChange = { onUpdate(personalInfo.copy(summary = it)) },
            placeholder = { Text("Ketikan secara singkat keterampilan menyeluruh atau pencapaian terbesar Anda. Ketuk tombol ✨ di atas agar AI membantu menyusun paragraf yang luar biasa!") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth().testTag("personal_summary_input")
        )
    }
}

// --- TAB 1: EXPERIENCES ---
@Composable
fun ExperiencesTab(
    experiences: List<Experience>,
    onAdd: (Experience) -> Unit,
    onUpdate: (Experience) -> Unit,
    onDelete: (String) -> Unit,
    onPolishExperience: (String, (String) -> Unit) -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pengalaman Kerja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cantumkan riwayat profesional Anda", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val new = Experience()
                        onAdd(new)
                        expandedId = new.id
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        if (experiences.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.WorkOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada pengalaman terdaftar", fontWeight = FontWeight.Medium)
                        Text("Ketuk 'Tambah' untuk menginput riwayat kerja.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(experiences, key = { it.id }) { exp ->
                ExperienceItemCard(
                    exp = exp,
                    isExpanded = expandedId == exp.id,
                    onToggleExpand = { expandedId = if (expandedId == exp.id) null else exp.id },
                    onUpdate = onUpdate,
                    onDelete = { onDelete(exp.id) },
                    onPolish = onPolishExperience
                )
            }
        }
    }
}

@Composable
fun ExperienceItemCard(
    exp: Experience,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (Experience) -> Unit,
    onDelete: () -> Unit,
    onPolish: (String, (String) -> Unit) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exp.jobTitle.ifEmpty { "Jabatan Belum Diisi" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = exp.company.ifEmpty { "Nama Perusahaan" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = exp.jobTitle,
                        onValueChange = { onUpdate(exp.copy(jobTitle = it)) },
                        label = { Text("Gelar Jabatan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = exp.company,
                        onValueChange = { onUpdate(exp.copy(company = it)) },
                        label = { Text("Perusahaan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = exp.startDate,
                            onValueChange = { onUpdate(exp.copy(startDate = it)) },
                            label = { Text("Tahun Mulai") },
                            placeholder = { Text("e.g. 2021") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = exp.endDate,
                            onValueChange = { onUpdate(exp.copy(endDate = it)) },
                            label = { Text("Tahun Selesai") },
                            placeholder = { Text("e.g. Sekarang") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tanggung Jawab & Prestasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        IconButton(
                            onClick = {
                                onPolish(exp.description) { polished ->
                                    onUpdate(exp.copy(description = polished))
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFFEF08A), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFCA8A04), modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedTextField(
                        value = exp.description,
                        onValueChange = { onUpdate(exp.copy(description = it)) },
                        placeholder = { Text("Tuliskan tugas harian atau prestasi penting Anda. Ketuk tombol ✨ di atas agar AI membantu membuat poin prestasi menarik.") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- TAB 2: EDUCATION ---
@Composable
fun EducationTab(
    educations: List<Education>,
    onAdd: (Education) -> Unit,
    onUpdate: (Education) -> Unit,
    onDelete: (String) -> Unit,
    onPolishEducation: (String, (String) -> Unit) -> Unit
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Riwayat Pendidikan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Cantumkan latar belakang edukasi Anda", style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val new = Education()
                        onAdd(new)
                        expandedId = new.id
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah")
                }
            }
        }

        if (educations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada riwayat pendidikan", fontWeight = FontWeight.Medium)
                        Text("Ketuk 'Tambah' untuk menginput riwayat sekolah.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(educations, key = { it.id }) { edu ->
                EducationItemCard(
                    edu = edu,
                    isExpanded = expandedId == edu.id,
                    onToggleExpand = { expandedId = if (expandedId == edu.id) null else edu.id },
                    onUpdate = onUpdate,
                    onDelete = { onDelete(edu.id) },
                    onPolish = onPolishEducation
                )
            }
        }
    }
}

@Composable
fun EducationItemCard(
    edu: Education,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (Education) -> Unit,
    onDelete: () -> Unit,
    onPolish: (String, (String) -> Unit) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = edu.institution.ifEmpty { "Nama Institusi Belum Diisi" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = edu.major.ifEmpty { "Jurusan / Studi" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Expand"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = edu.institution,
                        onValueChange = { onUpdate(edu.copy(institution = it)) },
                        label = { Text("Institusi / Sekolah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = edu.major,
                        onValueChange = { onUpdate(edu.copy(major = it)) },
                        label = { Text("Jurusan / Bidang Studi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = edu.startDate,
                            onValueChange = { onUpdate(edu.copy(startDate = it)) },
                            label = { Text("Tahun Mulai") },
                            placeholder = { Text("e.g. 2016") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = edu.endDate,
                            onValueChange = { onUpdate(edu.copy(endDate = it)) },
                            label = { Text("Tahun Lulus") },
                            placeholder = { Text("e.g. 2020") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keterangan Tambahan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        IconButton(
                            onClick = {
                                onPolish(edu.description) { polished ->
                                    onUpdate(edu.copy(description = polished))
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFFEF08A), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFCA8A04), modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedTextField(
                        value = edu.description,
                        onValueChange = { onUpdate(edu.copy(description = it)) },
                        placeholder = { Text("e.g. IPK, Organisasi, Prestasi penting.") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- TAB 3: SKILLS AND LANGUAGES ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsLanguagesTab(
    skills: List<CvSkill>,
    languages: List<CvLanguage>,
    onAddSkill: (CvSkill) -> Unit,
    onRemoveSkill: (String) -> Unit,
    onAddLanguage: (CvLanguage) -> Unit,
    onRemoveLanguage: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    var skillInput by remember { mutableStateOf("") }
    var skillLevel by remember { mutableStateOf("Sangat Mahir") }

    var langInput by remember { mutableStateOf("") }
    var langLevel by remember { mutableStateOf("Native / Fasih") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // SKILLS
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Keahlian Utama (Skills)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = skillInput,
                        onValueChange = { skillInput = it },
                        label = { Text("Keahlian") },
                        placeholder = { Text("e.g. Jetpack Compose") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )

                    // Simple select Box (Custom dropdown or static row of choices is easier)
                    var levelExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = skillLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tingkat") },
                            trailingIcon = {
                                IconButton(onClick = { levelExpanded = !levelExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable { levelExpanded = true }
                        )

                        DropdownMenu(
                            expanded = levelExpanded,
                            onDismissRequest = { levelExpanded = false }
                        ) {
                            listOf("Pemula", "Menengah", "Mahir", "Sangat Mahir").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        skillLevel = level
                                        levelExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (skillInput.isNotBlank()) {
                                onAddSkill(CvSkill(skillInput.trim(), skillLevel))
                                skillInput = ""
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Ketuk chip keahlian untuk menghapus:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveSkill(skill.name) },
                            label = { Text("${skill.name} (${skill.level})") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }
        }

        // LANGUAGES
        Card {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Penguasaan Bahasa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = langInput,
                        onValueChange = { langInput = it },
                        label = { Text("Bahasa") },
                        placeholder = { Text("e.g. Bahasa Inggris") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )

                    var langExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = langLevel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tingkat") },
                            trailingIcon = {
                                IconButton(onClick = { langExpanded = !langExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable { langExpanded = true }
                        )

                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false }
                        ) {
                            listOf("Pasif", "Aktif", "Profesional", "Native / Fasih").forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        langLevel = level
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (langInput.isNotBlank()) {
                                onAddLanguage(CvLanguage(langInput.trim(), langLevel))
                                langInput = ""
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Ketuk chip bahasa untuk menghapus:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveLanguage(lang.name) },
                            label = { Text("${lang.name} (${lang.proficiency})") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 4: EXPORT & REAL-TIME PREVIEW ---
@Composable
fun ExportTab(
    draft: CvDraft,
    viewModel: CvViewModel,
    onSelectTemplate: (TemplateType) -> Unit,
    onExportPdf: () -> Unit
) {
    val context = LocalContext.current
    val pagePreviewIndex by viewModel.selectedPagePreview.collectAsStateWithLifecycle()
    val totalPages = remember(draft) { PdfGenerator.calculateTotalPages(draft) }

    // Instant real-time preview render
    val bitmap = remember(draft, pagePreviewIndex) {
        PdfGenerator.generatePreviewBitmap(context, draft, pagePreviewIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Pilih Template Desain",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // Selectable Template List
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TemplateType.values()) { type ->
                val isSelected = draft.templateType == type.name
                val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

                Card(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { onSelectTemplate(type) }
                        .testTag("template_selector_${type.name}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when (type) {
                                        TemplateType.MODERN_TECH -> Color(0xFF1E3A8A)
                                        TemplateType.ELEGANT_MINIMAL -> Color(0xFF111827)
                                        TemplateType.CREATIVE_COLOR -> Color(0xFF0F766E)
                                        TemplateType.EXECUTIVE_CLASSIC -> Color(0xFF1E1B4B)
                                        else -> Color(0xFF374151)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            type.displayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Pratinjau Real-Time (WYSIWYG)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Halaman ${pagePreviewIndex + 1} dari $totalPages", style = MaterialTheme.typography.bodySmall)
            }

            if (totalPages > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledIconButton(
                        onClick = { viewModel.selectPage(0) },
                        enabled = pagePreviewIndex > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    FilledIconButton(
                        onClick = { viewModel.selectPage(1) },
                        enabled = pagePreviewIndex < totalPages - 1,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Live Rendered Canvas Bitmap
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(595f / 842f)
                .shadow(6.dp, RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF Live Page Preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onExportPdf,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("export_pdf_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan & Unduh Format PDF", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

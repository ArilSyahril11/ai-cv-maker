package com.example.nativeapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.model.*
import com.example.network.GeminiHelper
import com.example.utils.PdfGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    context: android.content.Context,
    draft: CVData,
    onBack: () -> Unit,
    onSave: (CVData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val geminiHelper = remember { GeminiHelper() }
    
    var title by remember { mutableStateOf(draft.title) }
    var template by remember { mutableStateOf(draft.template) }
    var accentColor by remember { mutableStateOf(draft.accentColor ?: "#06b6d4") }
    var personalData by remember { mutableStateOf(draft.personal) }
    var experiences by remember { mutableStateOf(draft.experiences) }
    var educations by remember { mutableStateOf(draft.educations) }
    var skills by remember { mutableStateOf(draft.skills) }
    
    // New fields
    var languages by remember { mutableStateOf(draft.languages) }
    var certifications by remember { mutableStateOf(draft.certifications) }
    var organizations by remember { mutableStateOf(draft.organizations) }
    var hobbies by remember { mutableStateOf(draft.hobbies) }
    
    // UI states
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Profil", "Pengalaman", "Pendidikan", "Keahlian", "Lainnya", "Pratinjau")
    
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text("Menu CV", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Simpan Draft CV") },
                    icon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val updatedDraft = draft.copy(
                            title = title,
                            template = template,
                            accentColor = accentColor,
                            personal = personalData,
                            experiences = experiences,
                            educations = educations,
                            skills = skills,
                            languages = languages,
                            certifications = certifications,
                            organizations = organizations,
                            hobbies = hobbies,
                            lastModified = System.currentTimeMillis()
                        )
                        onSave(updatedDraft)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Analisa ATS (AI)") },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B)) },
                    selected = false,
                    onClick = {
                        scope.launch { 
                            drawerState.close()
                            isAnalyzing = true
                            val cvText = StringBuilder()
                            cvText.append("Nama: ${personalData.fullName}\nTitle: ${personalData.title}\nSummary: ${personalData.summary}\nExperiences:\n")
                            experiences.forEach { cvText.append("- ${it.title} at ${it.company} (${it.description})\n") }
                            cvText.append("Education:\n")
                            educations.forEach { cvText.append("- ${it.major} at ${it.institution}\n") }
                            cvText.append("Skills: ${skills.joinToString { it.name }}\n")
                            
                            val result = geminiHelper.analyzeResume(cvText.toString())
                            analysisResult = result
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Pratinjau & Ekspor") },
                    icon = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedTab = 5
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title.ifEmpty { "Edit CV" }, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = label, 
                                fontSize = 13.sp, 
                                fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if(selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> ProfilTab(
                        title = title,
                        onTitleChange = { title = it },
                        personalData = personalData,
                        onPersonalChange = { personalData = it },
                        onAiPolish = { type, text ->
                            scope.launch {
                                val result = geminiHelper.polishText(type, text)
                                if (!result.startsWith("Error")) {
                                    personalData = personalData.copy(summary = result)
                                }
                            }
                        }
                    )
                    1 -> ExperiencesTab(
                        experiences = experiences,
                        onExperiencesChange = { experiences = it },
                        onAiPolish = { id, text ->
                            scope.launch {
                                val result = geminiHelper.polishText("experience", text)
                                if (!result.startsWith("Error")) {
                                    experiences = experiences.map { 
                                        if(it.id == id) it.copy(description = result) else it 
                                    }
                                }
                            }
                        }
                    )
                    2 -> EducationsTab(
                        educations = educations,
                        onEducationsChange = { educations = it }
                    )
                    3 -> SkillsTab(
                        skills = skills,
                        onSkillsChange = { skills = it }
                    )
                    4 -> LainnyaTab(
                        languages = languages,
                        onLanguagesChange = { languages = it },
                        certifications = certifications,
                        onCertificationsChange = { certifications = it },
                        organizations = organizations,
                        onOrganizationsChange = { organizations = it },
                        hobbies = hobbies,
                        onHobbiesChange = { hobbies = it }
                    )
                    5 -> PratinjauTab(
                        context = context,
                        draft = draft.copy(
                            title = title,
                            template = template,
                            accentColor = accentColor,
                            personal = personalData,
                            experiences = experiences,
                            educations = educations,
                            skills = skills,
                            languages = languages,
                            certifications = certifications,
                            organizations = organizations,
                            hobbies = hobbies
                        ),
                        currentTemplate = template,
                        onTemplateChange = { template = it },
                        currentAccentColor = accentColor,
                        onAccentColorChange = { accentColor = it }
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    }

    if (isAnalyzing) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Menganalisa CV...", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sistem ATS Gemini sedang memproses resume Anda...", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            confirmButton = { }
        )
    }

    if (analysisResult != null) {
        AlertDialog(
            onDismissRequest = { analysisResult = null },
            title = { Text("Hasil Analisa ATS", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(analysisResult ?: "")
                }
            },
            confirmButton = {
                Button(onClick = { analysisResult = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun PratinjauTab(
    context: android.content.Context,
    draft: CVData,
    currentTemplate: String,
    onTemplateChange: (String) -> Unit,
    currentAccentColor: String,
    onAccentColorChange: (String) -> Unit
) {
    val mappedDraft = com.example.data.CvDraft(
        draftTitle = draft.title,
        personalInfo = com.example.data.PersonalInfo(
            fullName = draft.personal.fullName,
            title = draft.personal.title,
            email = draft.personal.email,
            phone = draft.personal.phone,
            location = draft.personal.location,
            website = draft.personal.website,
            linkedin = draft.personal.linkedin,
            behance = draft.personal.behance,
            dribbble = draft.personal.dribbble,
            github = draft.personal.github,
            tiktok = draft.personal.tiktok,
            instagram = draft.personal.instagram,
            youtube = draft.personal.youtube,
            summary = draft.personal.summary
        ),
        experiences = draft.experiences.map { exp ->
            com.example.data.Experience(
                id = exp.id,
                jobTitle = exp.title,
                company = exp.company,
                startDate = exp.startDate,
                endDate = exp.endDate,
                description = exp.description
            )
        },
        educations = draft.educations.map { edu ->
            com.example.data.Education(
                id = edu.id,
                institution = edu.institution,
                major = edu.major,
                startDate = edu.startDate,
                endDate = edu.endDate,
                description = edu.description
            )
        },
        skills = draft.skills.map { skill ->
            com.example.data.CvSkill(name = skill.name, level = skill.level)
        },
        languages = draft.languages.map { lang ->
            com.example.data.CvLanguage(name = lang.name, proficiency = lang.proficiency)
        },
        certifications = draft.certifications.map { cert ->
            com.example.data.CvCertification(id = cert.id, name = cert.name, issuer = cert.issuer, year = cert.year)
        },
        organizations = draft.organizations.map { org ->
            com.example.data.CvOrganization(id = org.id, name = org.name, role = org.role, period = org.period)
        },
        hobbies = draft.hobbies.map { hobby ->
            com.example.data.CvHobby(name = hobby.name)
        },
        templateType = currentTemplate
    )

    val previewBitmap = remember(draft, currentTemplate, currentAccentColor) {
        PdfGenerator.generatePreviewBitmap(context, mappedDraft, 0, currentAccentColor).asImageBitmap()
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    PdfGenerator.generatePdf(mappedDraft, currentAccentColor, os)
                }
                android.widget.Toast.makeText(context, "CV Berhasil diekspor!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Gagal mengekspor PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        SectionTitle("Pratinjau CV")
        Image(
            bitmap = previewBitmap,
            contentDescription = "Preview",
            modifier = Modifier.fillMaxWidth().height(440.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Kustomisasi Tampilan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Template CV", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val templates = com.example.data.TemplateType.values()
        val currentIndex = templates.indexOfFirst { it.name == currentTemplate }.takeIf { it >= 0 } ?: 0
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { 
                val newIndex = if (currentIndex > 0) currentIndex - 1 else templates.size - 1
                onTemplateChange(templates[newIndex].name) 
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Sebelumnya", modifier = Modifier.size(24.dp))
            }
            Text(templates[currentIndex].displayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { 
                val newIndex = if (currentIndex < templates.size - 1) currentIndex + 1 else 0
                onTemplateChange(templates[newIndex].name) 
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Selanjutnya", modifier = Modifier.size(24.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Warna Aksen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val colors = listOf("#06b6d4" to Color(0xFF06b6d4), "#10b981" to Color(0xFF10b981), "#f59e0b" to Color(0xFFf59e0b), "#ef4444" to Color(0xFFef4444), "#6366f1" to Color(0xFF6366f1))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEach { (hex, color) ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, shape = RoundedCornerShape(20.dp))
                        .clickable { onAccentColorChange(hex) }
                        .padding(2.dp)
                ) {
                    if (currentAccentColor == hex) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                val cleanTitle = draft.title.ifEmpty { "CV_Baru" }.replace("\\s+".toRegex(), "_")
                createDocumentLauncher.launch("$cleanTitle.pdf") 
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan & Ekspor PDF", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfilTab(
    title: String,
    onTitleChange: (String) -> Unit,
    personalData: PersonalData,
    onPersonalChange: (PersonalData) -> Unit,
    onAiPolish: (String, String) -> Unit
) {
    SectionTitle("Informasi Dokumen")
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Judul Draft CV") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    SectionTitle("Foto Profil")
    
    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            onPersonalChange(personalData.copy(profilePhotoUri = result.uriContent.toString()))
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            cropLauncher.launch(
                CropImageContractOptions(uri, CropImageOptions().apply {
                    imageSourceIncludeGallery = false
                    imageSourceIncludeCamera = false
                    fixAspectRatio = false
                    guidelines = com.canhub.cropper.CropImageView.Guidelines.ON
                })
            )
        }
    }

    // NEW: Shape selection
    val shapes = listOf("circle" to "Lingkaran", "square" to "Persegi", "rounded" to "Melengkung")
    var expandedShape by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(
                    when (personalData.profilePhotoShape) {
                        "square" -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                        "rounded" -> androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        else -> androidx.compose.foundation.shape.CircleShape
                    }
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (personalData.profilePhotoUri != null) {
                AsyncImage(
                    model = personalData.profilePhotoUri,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Button(
                onClick = { launcher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("Pilih Foto", fontWeight = FontWeight.Bold)
            }
            if (personalData.profilePhotoUri != null) {
                TextButton(onClick = { onPersonalChange(personalData.copy(profilePhotoUri = null)) }) {
                    Text("Hapus Foto", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // Photo Settings
    if (personalData.profilePhotoUri != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Bentuk Bingkai Foto:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shapes.forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = personalData.profilePhotoShape == key,
                                onClick = { onPersonalChange(personalData.copy(profilePhotoShape = key)) }
                            )
                            Text(label, fontSize = 14.sp, modifier = Modifier.clickable { onPersonalChange(personalData.copy(profilePhotoShape = key)) })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ukuran Foto di PDF (Skala): ${"%.1f".format(personalData.profilePhotoScale)}x", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Slider(
                    value = personalData.profilePhotoScale,
                    onValueChange = { onPersonalChange(personalData.copy(profilePhotoScale = it)) },
                    valueRange = 0.5f..2.0f,
                    steps = 14
                )
            }
        }
    }

    SectionTitle("Informasi Pribadi")
    
    OutlinedTextField(
        value = personalData.fullName,
        onValueChange = { onPersonalChange(personalData.copy(fullName = it)) },
        label = { Text("Nama Lengkap") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = personalData.title,
        onValueChange = { onPersonalChange(personalData.copy(title = it)) },
        label = { Text("Gelar / Jabatan Profesional") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = personalData.email,
            onValueChange = { onPersonalChange(personalData.copy(email = it)) },
            label = { Text("Email") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = personalData.phone,
            onValueChange = { onPersonalChange(personalData.copy(phone = it)) },
            label = { Text("Telepon") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = personalData.location,
            onValueChange = { onPersonalChange(personalData.copy(location = it)) },
            label = { Text("Lokasi (Kota & Negara)") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = personalData.website,
            onValueChange = { onPersonalChange(personalData.copy(website = it)) },
            label = { Text("Website / Portfolio") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    SectionTitle("Media Sosial")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = personalData.linkedin,
            onValueChange = { onPersonalChange(personalData.copy(linkedin = it)) },
            label = { Text("LinkedIn") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = personalData.github,
            onValueChange = { onPersonalChange(personalData.copy(github = it)) },
            label = { Text("GitHub") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = personalData.behance,
            onValueChange = { onPersonalChange(personalData.copy(behance = it)) },
            label = { Text("Behance") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = personalData.dribbble,
            onValueChange = { onPersonalChange(personalData.copy(dribbble = it)) },
            label = { Text("Dribbble") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = personalData.instagram,
            onValueChange = { onPersonalChange(personalData.copy(instagram = it)) },
            label = { Text("Instagram") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = personalData.youtube,
            onValueChange = { onPersonalChange(personalData.copy(youtube = it)) },
            label = { Text("YouTube") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = personalData.tiktok,
        onValueChange = { onPersonalChange(personalData.copy(tiktok = it)) },
        label = { Text("TikTok") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Ringkasan Profil")
        Spacer(modifier = Modifier.weight(1f))
        AiPolishButton { onAiPolish("summary", personalData.summary) }
    }
    OutlinedTextField(
        value = personalData.summary,
        onValueChange = { onPersonalChange(personalData.copy(summary = it)) },
        label = { Text("Ceritakan tentang diri Anda...") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun ExperiencesTab(
    experiences: List<ExperienceData>,
    onExperiencesChange: (List<ExperienceData>) -> Unit,
    onAiPolish: (String, String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Riwayat Pekerjaan")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { 
            onExperiencesChange(experiences + ExperienceData(java.util.UUID.randomUUID().toString(), "", "", "", "", ""))
        }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Tambah")
        }
    }
    
    experiences.forEachIndexed { index, exp ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pekerjaan #${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onExperiencesChange(experiences.filter { it.id != exp.id }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                
                OutlinedTextField(
                    value = exp.title,
                    onValueChange = { newVal -> 
                        onExperiencesChange(experiences.map { if(it.id == exp.id) it.copy(title = newVal) else it }) 
                    },
                    label = { Text("Jabatan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exp.company,
                    onValueChange = { newVal -> 
                        onExperiencesChange(experiences.map { if(it.id == exp.id) it.copy(company = newVal) else it }) 
                    },
                    label = { Text("Perusahaan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = exp.startDate,
                        onValueChange = { newVal -> 
                            onExperiencesChange(experiences.map { if(it.id == exp.id) it.copy(startDate = newVal) else it }) 
                        },
                        label = { Text("Mulai") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = exp.endDate,
                        onValueChange = { newVal -> 
                            onExperiencesChange(experiences.map { if(it.id == exp.id) it.copy(endDate = newVal) else it }) 
                        },
                        label = { Text("Selesai") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tanggung Jawab", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    AiPolishButton { onAiPolish(exp.id, exp.description) }
                }
                OutlinedTextField(
                    value = exp.description,
                    onValueChange = { newVal -> 
                        onExperiencesChange(experiences.map { if(it.id == exp.id) it.copy(description = newVal) else it }) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun EducationsTab(
    educations: List<EducationData>,
    onEducationsChange: (List<EducationData>) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Riwayat Pendidikan")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { 
            onEducationsChange(educations + EducationData(java.util.UUID.randomUUID().toString(), "", "", "", "", ""))
        }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Tambah")
        }
    }
    
    educations.forEachIndexed { index, edu ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pendidikan #${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEducationsChange(educations.filter { it.id != edu.id }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
                
                OutlinedTextField(
                    value = edu.institution,
                    onValueChange = { newVal -> 
                        onEducationsChange(educations.map { if(it.id == edu.id) it.copy(institution = newVal) else it }) 
                    },
                    label = { Text("Nama Institusi") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = edu.major,
                    onValueChange = { newVal -> 
                        onEducationsChange(educations.map { if(it.id == edu.id) it.copy(major = newVal) else it }) 
                    },
                    label = { Text("Jurusan") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = edu.startDate,
                        onValueChange = { newVal -> 
                            onEducationsChange(educations.map { if(it.id == edu.id) it.copy(startDate = newVal) else it }) 
                        },
                        label = { Text("Mulai (Tahun)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = edu.endDate,
                        onValueChange = { newVal -> 
                            onEducationsChange(educations.map { if(it.id == edu.id) it.copy(endDate = newVal) else it }) 
                        },
                        label = { Text("Selesai (Tahun)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LainnyaTab(
    languages: List<LanguageData>,
    onLanguagesChange: (List<LanguageData>) -> Unit,
    certifications: List<CertificationData>,
    onCertificationsChange: (List<CertificationData>) -> Unit,
    organizations: List<OrganizationData>,
    onOrganizationsChange: (List<OrganizationData>) -> Unit,
    hobbies: List<HobbyData>,
    onHobbiesChange: (List<HobbyData>) -> Unit
) {
    // Organisasi
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Organisasi")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { onOrganizationsChange(organizations + OrganizationData(java.util.UUID.randomUUID().toString(), "", "", "")) }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Tambah")
        }
    }
    organizations.forEach { org ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = org.name, onValueChange = { n -> onOrganizationsChange(organizations.map { if(it.id == org.id) it.copy(name = n) else it }) }, label = { Text("Nama Organisasi") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    IconButton(onClick = { onOrganizationsChange(organizations.filter { it.id != org.id }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = org.role, onValueChange = { r -> onOrganizationsChange(organizations.map { if(it.id == org.id) it.copy(role = r) else it }) }, label = { Text("Peran / Jabatan") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = org.period, onValueChange = { p -> onOrganizationsChange(organizations.map { if(it.id == org.id) it.copy(period = p) else it }) }, label = { Text("Periode (Tahun)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Sertifikasi
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Sertifikasi")
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { onCertificationsChange(certifications + CertificationData(java.util.UUID.randomUUID().toString(), "", "", "")) }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Tambah")
        }
    }
    certifications.forEach { cert ->
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = cert.name, onValueChange = { n -> onCertificationsChange(certifications.map { if(it.id == cert.id) it.copy(name = n) else it }) }, label = { Text("Nama Sertifikasi") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    IconButton(onClick = { onCertificationsChange(certifications.filter { it.id != cert.id }) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cert.issuer, onValueChange = { i -> onCertificationsChange(certifications.map { if(it.id == cert.id) it.copy(issuer = i) else it }) }, label = { Text("Penerbit") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = cert.year, onValueChange = { y -> onCertificationsChange(certifications.map { if(it.id == cert.id) it.copy(year = y) else it }) }, label = { Text("Tahun") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp))
                }
            }
        }
    }
}

@Composable
fun SkillsTab(
    skills: List<SkillData>,
    onSkillsChange: (List<SkillData>) -> Unit
) {
    SectionTitle("Keahlian Utama")
    var newSkillName by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newSkillName,
            onValueChange = { newSkillName = it },
            label = { Text("Contoh: Jetpack Compose") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { 
                if(newSkillName.isNotBlank()) {
                    onSkillsChange(skills + SkillData(newSkillName, "Menengah"))
                    newSkillName = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Tambah")
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    skills.forEach { skill ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(skill.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSkillsChange(skills.filter { it.name != skill.name }) }) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun AiPolishButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("AI Polish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

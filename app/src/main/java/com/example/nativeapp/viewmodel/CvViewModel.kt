package com.example.nativeapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativeapp.data.AppDatabase
import com.example.nativeapp.data.CvEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CvViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = com.example.model.CVRepository(application)

    val allDrafts: StateFlow<List<com.example.model.CVData>> = repository.getAllDrafts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewDraft() {
        viewModelScope.launch {
            val newDraft = com.example.model.CVData(
                id = java.util.UUID.randomUUID().toString(),
                title = "CV Baru",
                lastModified = System.currentTimeMillis(),
                template = "modern"
            )
            repository.insertDraft(newDraft)
        }
    }

    fun deleteDraft(data: com.example.model.CVData) {
        viewModelScope.launch {
            repository.deleteDraft(data)
        }
    }

    fun updateDraft(data: com.example.model.CVData) {
        viewModelScope.launch {
            repository.updateDraft(data)
        }
    }

    fun importDraft(data: com.example.model.CVData) {
        viewModelScope.launch {
            val newDraft = data.copy(
                id = java.util.UUID.randomUUID().toString(),
                lastModified = System.currentTimeMillis()
            )
            repository.insertDraft(newDraft)
        }
    }

    fun generateDummyCv() {
        viewModelScope.launch {
            val dummyId = java.util.UUID.randomUUID().toString()
            val dummyDraft = com.example.model.CVData(
                id = dummyId,
                title = "Contoh CV: Software Engineer",
                lastModified = System.currentTimeMillis(),
                template = "modern_tech",
                accentColor = "#6366f1",
                personal = com.example.model.PersonalData(
                    fullName = "Budi Hartono",
                    title = "Senior Software Engineer",
                    email = "budi.hartono@email.com",
                    phone = "+62 812-3456-7890",
                    location = "Jakarta, Indonesia",
                    website = "budihartono.dev",
                    linkedin = "linkedin.com/in/budihartono",
                    summary = "Software Engineer berpengalaman dengan keahlian mendalam dalam pengembangan aplikasi mobile dan web skala besar. Menguasai Kotlin, Jetpack Compose, dan arsitektur microservices. Terbiasa memimpin tim lintas divisi untuk mengantarkan produk digital berkualitas tinggi tepat waktu."
                ),
                experiences = listOf(
                    com.example.model.ExperienceData(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "Senior Android Developer",
                        company = "Tech Nusantara Gemilang",
                        startDate = "Jan 2021",
                        endDate = "Sekarang",
                        description = "Memimpin pengembangan aplikasi utama e-commerce yang digunakan oleh lebih dari 5 juta pengguna aktif. Menerapkan arsitektur MVI dengan Jetpack Compose yang meningkatkan performa rendering UI hingga 40%. Mengurangi angka crash rate dari 2.5% menjadi 0.1% dalam 3 bulan."
                    ),
                    com.example.model.ExperienceData(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "Android Developer",
                        company = "Startup Digital Inovasi",
                        startDate = "Mar 2018",
                        endDate = "Des 2020",
                        description = "Mengembangkan fitur fintech untuk transaksi P2P lending. Berkolaborasi dengan tim backend untuk mengintegrasikan RESTful API dan WebSocket untuk notifikasi real-time."
                    )
                ),
                educations = listOf(
                    com.example.model.EducationData(
                        id = java.util.UUID.randomUUID().toString(),
                        institution = "Institut Teknologi Bandung",
                        major = "Teknik Informatika",
                        startDate = "2014",
                        endDate = "2018",
                        description = "Lulus dengan predikat Cum Laude (IPK: 3.85). Aktif dalam himpunan mahasiswa dan kompetisi competitive programming."
                    )
                ),
                skills = listOf(
                    com.example.model.SkillData("Kotlin", "Sangat Mahir"),
                    com.example.model.SkillData("Jetpack Compose", "Sangat Mahir"),
                    com.example.model.SkillData("Android SDK", "Mahir"),
                    com.example.model.SkillData("Coroutines & Flow", "Mahir"),
                    com.example.model.SkillData("Git & CI/CD", "Menengah")
                ),
                languages = listOf(
                    com.example.model.LanguageData("Bahasa Indonesia", "Native"),
                    com.example.model.LanguageData("Bahasa Inggris", "Profesional Aktif")
                ),
                certifications = listOf(
                    com.example.model.CertificationData(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "Associate Android Developer",
                        issuer = "Google",
                        year = "2020"
                    )
                )
            )
            repository.insertDraft(dummyDraft)
        }
    }
}

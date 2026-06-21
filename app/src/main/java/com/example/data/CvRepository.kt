package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class CvRepository(private val cvDao: CvDao) {
    val allCvDrafts: Flow<List<CvDraft>> = cvDao.getAllCvDrafts()

    fun getCvDraftById(id: Int): Flow<CvDraft?> = cvDao.getCvDraftById(id)

    suspend fun getCvDraftByIdDirect(id: Int): CvDraft? = cvDao.getCvDraftByIdDirect(id)

    suspend fun saveCvDraft(cvDraft: CvDraft): Int {
        val draftToSave = cvDraft.copy(lastModified = System.currentTimeMillis())
        val id = if (cvDraft.id == 0) {
            cvDao.insertCvDraft(draftToSave).toInt()
        } else {
            cvDao.updateCvDraft(draftToSave)
            cvDraft.id
        }
        return id
    }

    suspend fun deleteCvDraft(id: Int) {
        cvDao.deleteCvDraftById(id)
    }

    suspend fun populateSampleIfEmpty() {
        val currentDrafts = allCvDrafts.firstOrNull()
        if (currentDrafts.isNullOrEmpty()) {
            val sampleExperienceList = listOf(
                Experience(
                    id = UUID.randomUUID().toString(),
                    jobTitle = "Senior Android Engineer",
                    company = "Tech Indo Solution",
                    startDate = "2022",
                    endDate = "Sekarang",
                    description = "Memimpin pengembangan aplikasi belanja Android dengan 500k+ unduhan. Mengurangi crash rate aplikasi hingga 0.05% dengan menerapkan arsitektur MVVM, asinkronitas Coroutines, dan menyederhanakan kode pakai Jetpack Compose."
                ),
                Experience(
                    id = UUID.randomUUID().toString(),
                    jobTitle = "Android Developer",
                    company = "Maju Jaya Digital",
                    startDate = "2020",
                    endDate = "2022",
                    description = "Mengembangkan fitur utama aplikasi perbankan termasuk integrasi gerbang pembayaran, enkripsi data sensitif pengguna, serta sinkronisasi data offline menggunakan database Room."
                )
            )

            val sampleEducationList = listOf(
                Education(
                    id = UUID.randomUUID().toString(),
                    institution = "Universitas Indonesia",
                    major = "S1 Teknik Informatika",
                    startDate = "2016",
                    endDate = "2020",
                    description = "Lulus dengan predikat Cum Laude (IPK 3.85). Fokus pada Rekayasa Perangkat Lunak dan Jaringan Komputer."
                )
            )

            val sampleSkills = listOf(
                CvSkill("Kotlin & Java", "Sangat Mahir"),
                CvSkill("Jetpack Compose", "Sangat Mahir"),
                CvSkill("Android SDK & Clean Architecture", "Mahir"),
                CvSkill("Room DB & SQLite", "Mahir"),
                CvSkill("Rest API & Retrofit", "Mahir"),
                CvSkill("Git & CI/CD", "Menengah")
            )

            val sampleLanguages = listOf(
                CvLanguage("Bahasa Indonesia", "Native / Fasih"),
                CvLanguage("Bahasa Inggris", "Profesional (TOEFL 580)")
            )

            val firstSample = CvDraft(
                draftTitle = "CV Utama - Android Developer",
                personalInfo = PersonalInfo(
                    fullName = "Budi Hartono",
                    title = "Senior Android Developer",
                    email = "budi.hartono@email.com",
                    phone = "+62 812-3456-7890",
                    location = "Jakarta, Indonesia",
                    website = "linkedin.com/in/budihartono",
                    summary = "Software engineer profesional yang berfokus pada pengembangan aplikasi Android berkemampuan tinggi. Sangat menyukai penulisan kode yang bersih (clean code) dan arsitektur modular yang mudah diuji."
                ),
                experiences = sampleExperienceList,
                educations = sampleEducationList,
                skills = sampleSkills,
                languages = sampleLanguages,
                templateType = TemplateType.MODERN_TECH.name
            )

            cvDao.insertCvDraft(firstSample)
        }
    }
}

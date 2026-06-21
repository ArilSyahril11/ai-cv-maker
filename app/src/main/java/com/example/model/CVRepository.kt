package com.example.model

import android.content.Context
import com.example.nativeapp.data.AppDatabase
import com.example.nativeapp.data.CvEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CVRepository(context: Context) {
    private val cvDao = AppDatabase.getDatabase(context).cvDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private val personalAdapter = moshi.adapter(PersonalData::class.java)
    private val expListAdapter = moshi.adapter<List<ExperienceData>>(Types.newParameterizedType(List::class.java, ExperienceData::class.java))
    private val eduListAdapter = moshi.adapter<List<EducationData>>(Types.newParameterizedType(List::class.java, EducationData::class.java))
    private val skillListAdapter = moshi.adapter<List<SkillData>>(Types.newParameterizedType(List::class.java, SkillData::class.java))
    private val langListAdapter = moshi.adapter<List<LanguageData>>(Types.newParameterizedType(List::class.java, LanguageData::class.java))
    private val certListAdapter = moshi.adapter<List<CertificationData>>(Types.newParameterizedType(List::class.java, CertificationData::class.java))
    private val orgListAdapter = moshi.adapter<List<OrganizationData>>(Types.newParameterizedType(List::class.java, OrganizationData::class.java))
    private val hobbyListAdapter = moshi.adapter<List<HobbyData>>(Types.newParameterizedType(List::class.java, HobbyData::class.java))

    fun getAllDrafts(): Flow<List<CVData>> {
        return cvDao.getAllDrafts().map { entities ->
            entities.map { entity -> mapEntityToData(entity) }
        }
    }

    suspend fun insertDraft(data: CVData) {
        cvDao.insertDraft(mapDataToEntity(data))
    }

    suspend fun updateDraft(data: CVData) {
        cvDao.updateDraft(mapDataToEntity(data))
    }

    suspend fun deleteDraft(data: CVData) {
        cvDao.deleteDraft(mapDataToEntity(data))
    }

    private fun mapEntityToData(entity: CvEntity): CVData {
        return CVData(
            id = entity.id,
            title = entity.title,
            lastModified = entity.lastUpdated,
            template = entity.templateId,
            personal = try { personalAdapter.fromJson(entity.personalJson) ?: PersonalData() } catch (e: Exception) { PersonalData() },
            experiences = try { expListAdapter.fromJson(entity.experiencesJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            educations = try { eduListAdapter.fromJson(entity.educationsJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            skills = try { skillListAdapter.fromJson(entity.skillsJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            languages = try { langListAdapter.fromJson(entity.languagesJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            certifications = try { certListAdapter.fromJson(entity.certificationsJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            organizations = try { orgListAdapter.fromJson(entity.organizationsJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            hobbies = try { hobbyListAdapter.fromJson(entity.hobbiesJson) ?: emptyList() } catch (e: Exception) { emptyList() },
            accentColor = entity.accentColor
        )
    }

    private fun mapDataToEntity(data: CVData): CvEntity {
        return CvEntity(
            id = data.id,
            title = data.title,
            lastUpdated = data.lastModified,
            templateId = data.template,
            personalJson = personalAdapter.toJson(data.personal),
            experiencesJson = expListAdapter.toJson(data.experiences),
            educationsJson = eduListAdapter.toJson(data.educations),
            skillsJson = skillListAdapter.toJson(data.skills),
            languagesJson = langListAdapter.toJson(data.languages),
            certificationsJson = certListAdapter.toJson(data.certifications),
            organizationsJson = orgListAdapter.toJson(data.organizations),
            hobbiesJson = hobbyListAdapter.toJson(data.hobbies),
            accentColor = data.accentColor ?: "#06b6d4"
        )
    }
}

package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class TypeConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromPersonalInfo(personalInfo: PersonalInfo?): String {
        return personalInfo?.let {
            moshi.adapter(PersonalInfo::class.java).toJson(it)
        } ?: "{}"
    }

    @TypeConverter
    fun toPersonalInfo(json: String): PersonalInfo {
        return moshi.adapter(PersonalInfo::class.java).fromJson(json) ?: PersonalInfo()
    }

    @TypeConverter
    fun fromExperiences(list: List<Experience>?): String {
        val type = Types.newParameterizedType(List::class.java, Experience::class.java)
        val adapter = moshi.adapter<List<Experience>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toExperiences(json: String): List<Experience> {
        val type = Types.newParameterizedType(List::class.java, Experience::class.java)
        val adapter = moshi.adapter<List<Experience>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromEducations(list: List<Education>?): String {
        val type = Types.newParameterizedType(List::class.java, Education::class.java)
        val adapter = moshi.adapter<List<Education>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toEducations(json: String): List<Education> {
        val type = Types.newParameterizedType(List::class.java, Education::class.java)
        val adapter = moshi.adapter<List<Education>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromSkills(list: List<CvSkill>?): String {
        val type = Types.newParameterizedType(List::class.java, CvSkill::class.java)
        val adapter = moshi.adapter<List<CvSkill>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toSkills(json: String): List<CvSkill> {
        val type = Types.newParameterizedType(List::class.java, CvSkill::class.java)
        val adapter = moshi.adapter<List<CvSkill>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromLanguages(list: List<CvLanguage>?): String {
        val type = Types.newParameterizedType(List::class.java, CvLanguage::class.java)
        val adapter = moshi.adapter<List<CvLanguage>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toLanguages(json: String): List<CvLanguage> {
        val type = Types.newParameterizedType(List::class.java, CvLanguage::class.java)
        val adapter = moshi.adapter<List<CvLanguage>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromCertifications(list: List<CvCertification>?): String {
        val type = Types.newParameterizedType(List::class.java, CvCertification::class.java)
        val adapter = moshi.adapter<List<CvCertification>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toCertifications(json: String): List<CvCertification> {
        val type = Types.newParameterizedType(List::class.java, CvCertification::class.java)
        val adapter = moshi.adapter<List<CvCertification>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromOrganizations(list: List<CvOrganization>?): String {
        val type = Types.newParameterizedType(List::class.java, CvOrganization::class.java)
        val adapter = moshi.adapter<List<CvOrganization>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toOrganizations(json: String): List<CvOrganization> {
        val type = Types.newParameterizedType(List::class.java, CvOrganization::class.java)
        val adapter = moshi.adapter<List<CvOrganization>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromHobbies(list: List<CvHobby>?): String {
        val type = Types.newParameterizedType(List::class.java, CvHobby::class.java)
        val adapter = moshi.adapter<List<CvHobby>>(type)
        return list?.let { adapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toHobbies(json: String): List<CvHobby> {
        val type = Types.newParameterizedType(List::class.java, CvHobby::class.java)
        val adapter = moshi.adapter<List<CvHobby>>(type)
        return adapter.fromJson(json) ?: emptyList()
    }
}

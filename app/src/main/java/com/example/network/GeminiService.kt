package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiHelper {
    suspend fun polishText(type: String, originalText: String, customApiKey: String? = null): String {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            return "Error: Kunci API Gemini tidak valid atau belum diatur. Buat file .env di folder root proyek dengan GEMINI_API_KEY=kunci_anda, atau atur API Key di menu Pengaturan."
        }

        val prompt = when (type) {
            "summary" -> """
                Anda adalah asisten penulisan resume profesional. Tulis ulang ringkasan profil profesional (about me) berikut agar terdengar sangat menarik bagi rekruter HR.
                Gunakan bahasa yang profesional, percaya diri, dan berorientasi pada pencapaian. Buat dalam 2-3 kalimat yang solid dan bermakna tinggi.
                Teks asli: "$originalText"
                Hasil dalam Bahasa Indonesia yang formal dan profesional:
            """.trimIndent()
            "experience" -> """
                Anda adalah asisten penulisan resume profesional. Tulis ulang deskripsi pengalaman kerja berikut ke dalam poin-poin pencapaian profesional yang singkat, padat, dan berdampak tinggi.
                Gunakan action verbs (kata kerja aksi), sebutkan metrik/kemungkinan dampak jika relevan, dan hindari kata-kata klise.
                Teks asli: "$originalText"
                Hasil dalam Bahasa Indonesia yang formal dan profesional (berupa 2-3 poin dengan format baris baru, tanpa karakter aneh):
            """.trimIndent()
            "education" -> """
                Anda adalah asisten penulisan resume profesional. Ringkas atau tulis ulang deskripsi pendidikan berikut agar terdengar formal, rapi, dan relevan dengan industri kerja.
                Teks asli: "$originalText"
                Hasil dalam Bahasa Indonesia yang formal:
            """.trimIndent()
            else -> """
                Tulis ulang teks berikut agar terdengar lebih profesional dan menarik untuk CV/Resume kerja.
                Teks asli: "$originalText"
                Hasil dalam Bahasa Indonesia formal:
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah pakar HR dan penulis CV profesional. Tugas Anda adalah membantu menulis entri CV yang pendek, menarik, berdampak tinggi dan bebas kesalahan tata bahasa."))
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val rewritten = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            rewritten?.trim() ?: "Gagal mendapatkan hasil pemolesan teks."
        } catch (e: Exception) {
            "Gagal menghubungi server AI: ${e.localizedMessage ?: e.message}"
        }
    }

    suspend fun analyzeResume(resumeContent: String, customApiKey: String? = null): String {
        val apiKey = customApiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            return "Error: Kunci API Gemini tidak valid atau belum diatur. Buat file .env di folder root proyek dengan GEMINI_API_KEY=kunci_anda, atau atur API Key di menu Pengaturan."
        }

        val prompt = """
            Anda adalah seorang Senior HR Recruiter dan sistem ATS (Applicant Tracking System). 
            Berikut adalah data CV/Resume kandidat:
            
            $resumeContent
            
            Berikan analisis singkat dan mendalam dengan format persis seperti di bawah ini, TANPA TAMBAHAN KATA APAPUN:
            
            SKOR: [Angka 0-100]
            
            KELEBIHAN:
            - [Kelebihan 1]
            - [Kelebihan 2]
            
            SARAN PERBAIKAN:
            - [Saran 1]
            - [Saran 2]
            - [Saran 3]
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Anda adalah sistem penganalisa ATS dan pakar HR internasional. Jawablah sesuai format yang diminta dengan bahasa Indonesia formal."))
            )
        )

        return try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            result?.trim() ?: "Gagal menganalisa CV."
        } catch (e: Exception) {
            "Gagal menghubungi server AI: ${e.localizedMessage ?: e.message}"
        }
    }
}

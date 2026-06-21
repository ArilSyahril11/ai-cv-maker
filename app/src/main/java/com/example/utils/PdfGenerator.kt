package com.example.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import com.example.data.CvDraft
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGIN = 40f

    fun generatePdf(draft: CvDraft, accentColorHex: String?, outputStream: OutputStream, context: Context? = null) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = document.startPage(pageInfo)
        
        drawPage(page.canvas, draft, accentColorHex, context)
        
        document.finishPage(page)
        
        try {
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    fun generatePreviewBitmap(context: Context, draft: CvDraft, pageIndex: Int, accentColorHex: String? = null): Bitmap {
        val bitmap = Bitmap.createBitmap(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawPage(canvas, draft, accentColorHex, context)
        return bitmap
    }

    fun generateAndShare(context: Context, data: com.example.model.CVData) {
        val mappedDraft = CvDraft(
            draftTitle = data.title,
            personalInfo = com.example.data.PersonalInfo(
                fullName = data.personal.fullName,
                title = data.personal.title,
                profilePhotoUri = data.personal.profilePhotoUri,
                profilePhotoShape = data.personal.profilePhotoShape,
                profilePhotoScale = data.personal.profilePhotoScale,
                email = data.personal.email,
                phone = data.personal.phone,
                location = data.personal.location,
                website = data.personal.website,
                summary = data.personal.summary
            ),
            experiences = data.experiences.map { exp ->
                com.example.data.Experience(
                    id = exp.id,
                    jobTitle = exp.title,
                    company = exp.company,
                    startDate = exp.startDate,
                    endDate = exp.endDate,
                    description = exp.description
                )
            },
            educations = data.educations.map { edu ->
                com.example.data.Education(
                    id = edu.id,
                    institution = edu.institution,
                    major = edu.major,
                    startDate = edu.startDate,
                    endDate = edu.endDate,
                    description = edu.description
                )
            },
            skills = data.skills.map { skill ->
                com.example.data.CvSkill(name = skill.name, level = skill.level)
            },
            templateType = data.template
        )
        
        val fileName = "${data.title.ifEmpty { "CV" }.replace(" ", "_")}.pdf"
        val file = java.io.File(context.cacheDir, fileName)
        java.io.FileOutputStream(file).use { output ->
            generatePdf(mappedDraft, data.accentColor, output, context)
        }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Bagikan CV PDF"))
    }

    private fun drawPage(canvas: Canvas, draft: CvDraft, accentColorHex: String?, context: Context?) {
        val parsedColor = try {
            Color.parseColor(accentColorHex ?: "#6366F1")
        } catch (e: Exception) {
            Color.parseColor("#6366F1")
        }

        val type = draft.templateType

        when (type) {
            "MODERN_TECH", "PROFESSIONAL_CLEAN", "CORPORATE_STANDARD" -> {
                drawTwoColumnLayout(canvas, draft, parsedColor, context, isSidebarLeft = true)
            }
            "CREATIVE_PORTFOLIO", "ELEGANT_MINIMAL" -> {
                drawTwoColumnLayout(canvas, draft, parsedColor, context, isSidebarLeft = false)
            }
            "CREATIVE_COLOR" -> {
                drawTopColorBlockLayout(canvas, draft, parsedColor, context)
            }
            "BOLD_TYPOGRAPHY", "EXECUTIVE_CLASSIC", "ACADEMIC_SCHOLAR", "SIMPLE_MINIMALIST" -> {
                drawCenteredLayout(canvas, draft, parsedColor, context, isBold = (type == "BOLD_TYPOGRAPHY"))
            }
            else -> {
                drawTwoColumnLayout(canvas, draft, parsedColor, context, isSidebarLeft = true)
            }
        }
    }

    private fun loadProfilePhoto(context: Context?, uriString: String?, size: Float, shape: String): Bitmap? {
        if (context == null || uriString.isNullOrEmpty()) return null
        return try {
            val uri = Uri.parse(uriString)
            val original = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            
            // Calculate scale to fill the crop area (center crop)
            val scale = Math.max(size / original.width, size / original.height)
            val scaledWidth = original.width * scale
            val scaledHeight = original.height * scale
            val left = (size - scaledWidth) / 2f
            val top = (size - scaledHeight) / 2f
            
            val output = Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint().apply { isAntiAlias = true }
            val rect = RectF(0f, 0f, size, size)
            
            when (shape.lowercase()) {
                "square" -> canvas.drawRect(rect, paint)
                "rounded" -> canvas.drawRoundRect(rect, size / 8f, size / 8f, paint)
                else -> canvas.drawRoundRect(rect, size / 2f, size / 2f, paint) // default circle
            }
            
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            
            val dstRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(original, null, dstRect, paint)
            output
        } catch (e: Exception) {
            null
        }
    }

    private fun drawTwoColumnLayout(canvas: Canvas, draft: CvDraft, accentColor: Int, context: Context?, isSidebarLeft: Boolean) {
        val sidebarWidth = 200f
        val paint = Paint().apply { isAntiAlias = true }
        
        // Draw Sidebar Background if needed (e.g. slight gray or accent)
        paint.color = Color.parseColor("#F8FAFC")
        if (isSidebarLeft) {
            canvas.drawRect(0f, 0f, sidebarWidth, PAGE_HEIGHT, paint)
        } else {
            canvas.drawRect(PAGE_WIDTH - sidebarWidth, 0f, PAGE_WIDTH, PAGE_HEIGHT, paint)
        }

        val mainStartX = if (isSidebarLeft) sidebarWidth + 20f else MARGIN
        val sideStartX = if (isSidebarLeft) 20f else PAGE_WIDTH - sidebarWidth + 20f

        var mainY = MARGIN
        var sideY = MARGIN

        // Profile Photo
        val baseSize = 100f
        val photoSize = baseSize * draft.personalInfo.profilePhotoScale
        val photo = loadProfilePhoto(context, draft.personalInfo.profilePhotoUri, photoSize, draft.personalInfo.profilePhotoShape)
        if (photo != null) {
            canvas.drawBitmap(photo, sideStartX + (sidebarWidth - 40f - photoSize) / 2, sideY, null)
            sideY += photoSize + 20f
        }

        // Sidebar Content
        val sideTextPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isAntiAlias = true }
        val sideTitlePaint = Paint().apply { color = accentColor; textSize = 12f; isFakeBoldText = true; isAntiAlias = true }

        // Contact
        canvas.drawText("KONTAK", sideStartX, sideY, sideTitlePaint); sideY += 15f
        if (draft.personalInfo.phone.isNotEmpty()) { canvas.drawText(draft.personalInfo.phone, sideStartX, sideY, sideTextPaint); sideY += 15f }
        if (draft.personalInfo.email.isNotEmpty()) { canvas.drawText(draft.personalInfo.email, sideStartX, sideY, sideTextPaint); sideY += 15f }
        if (draft.personalInfo.location.isNotEmpty()) { canvas.drawText(draft.personalInfo.location, sideStartX, sideY, sideTextPaint); sideY += 15f }
        if (draft.personalInfo.website.isNotEmpty()) { canvas.drawText(draft.personalInfo.website, sideStartX, sideY, sideTextPaint); sideY += 15f }
        sideY += 15f

        // Skills
        if (draft.skills.isNotEmpty()) {
            canvas.drawText("KEAHLIAN", sideStartX, sideY, sideTitlePaint); sideY += 15f
            draft.skills.forEach {
                canvas.drawText("• ${it.name}", sideStartX, sideY, sideTextPaint); sideY += 15f
            }
            sideY += 15f
        }

        // Languages
        if (draft.languages.isNotEmpty()) {
            canvas.drawText("BAHASA", sideStartX, sideY, sideTitlePaint); sideY += 15f
            draft.languages.forEach {
                canvas.drawText("• ${it.name} (${it.proficiency})", sideStartX, sideY, sideTextPaint); sideY += 15f
            }
        }

        // Main Content
        val namePaint = Paint().apply { color = accentColor; textSize = 24f; isFakeBoldText = true; isAntiAlias = true }
        val titlePaint = Paint().apply { color = Color.GRAY; textSize = 14f; isAntiAlias = true }
        val sectionTitlePaint = Paint().apply { color = accentColor; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val datePaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }

        canvas.drawText(draft.personalInfo.fullName.ifEmpty { "Nama Lengkap" }, mainStartX, mainY, namePaint)
        mainY += 20f
        canvas.drawText(draft.personalInfo.title.ifEmpty { "Jabatan Profesional" }, mainStartX, mainY, titlePaint)
        mainY += 25f

        if (draft.personalInfo.summary.isNotEmpty()) {
            canvas.drawText("RINGKASAN", mainStartX, mainY, sectionTitlePaint); mainY += 15f
            val lines = draft.personalInfo.summary.chunked(70)
            lines.forEach { canvas.drawText(it, mainStartX, mainY, bodyPaint); mainY += 14f }
            mainY += 15f
        }

        if (draft.experiences.isNotEmpty()) {
            canvas.drawText("PENGALAMAN KERJA", mainStartX, mainY, sectionTitlePaint); mainY += 15f
            draft.experiences.forEach { exp ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText("${exp.jobTitle} - ${exp.company}", mainStartX, mainY, bodyPaint); mainY += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${exp.startDate} - ${exp.endDate}", mainStartX, mainY, datePaint); mainY += 14f
                val descLines = exp.description.chunked(75)
                descLines.forEach { canvas.drawText(it, mainStartX + 10f, mainY, bodyPaint); mainY += 14f }
                mainY += 10f
            }
        }

        if (draft.educations.isNotEmpty()) {
            canvas.drawText("PENDIDIKAN", mainStartX, mainY, sectionTitlePaint); mainY += 15f
            draft.educations.forEach { edu ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText(edu.institution, mainStartX, mainY, bodyPaint); mainY += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${edu.major} | ${edu.startDate} - ${edu.endDate}", mainStartX, mainY, datePaint); mainY += 14f
                val descLines = edu.description.chunked(75)
                descLines.forEach { canvas.drawText(it, mainStartX + 10f, mainY, bodyPaint); mainY += 14f }
                mainY += 10f
            }
        }
    }

    private fun drawTopColorBlockLayout(canvas: Canvas, draft: CvDraft, accentColor: Int, context: Context?) {
        val paint = Paint().apply { isAntiAlias = true }
        
        // Top Block
        paint.color = accentColor
        canvas.drawRect(0f, 0f, PAGE_WIDTH, 160f, paint)

        val namePaint = Paint().apply { color = Color.WHITE; textSize = 26f; isFakeBoldText = true; isAntiAlias = true }
        val titlePaint = Paint().apply { color = Color.parseColor("#E0E7FF"); textSize = 16f; isAntiAlias = true }
        
        var y = 60f
        canvas.drawText(draft.personalInfo.fullName.ifEmpty { "Nama Lengkap" }, MARGIN, y, namePaint); y += 25f
        canvas.drawText(draft.personalInfo.title.ifEmpty { "Jabatan Profesional" }, MARGIN, y, titlePaint); y += 30f

        val contactPaint = Paint().apply { color = Color.WHITE; textSize = 10f; isAntiAlias = true }
        val contacts = listOf(draft.personalInfo.email, draft.personalInfo.phone, draft.personalInfo.location).filter { it.isNotBlank() }
        canvas.drawText(contacts.joinToString(" | "), MARGIN, y, contactPaint)

        // Profile Photo overlapping
        val baseSize = 100f
        val photoSize = baseSize * draft.personalInfo.profilePhotoScale
        val photo = loadProfilePhoto(context, draft.personalInfo.profilePhotoUri, photoSize, draft.personalInfo.profilePhotoShape)
        if (photo != null) {
            canvas.drawBitmap(photo, PAGE_WIDTH - MARGIN - photoSize, 80f, null)
        }

        y = 190f
        val sectionTitlePaint = Paint().apply { color = accentColor; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val datePaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }

        if (draft.personalInfo.summary.isNotEmpty()) {
            canvas.drawText("RINGKASAN", MARGIN, y, sectionTitlePaint); y += 15f
            val lines = draft.personalInfo.summary.chunked(100)
            lines.forEach { canvas.drawText(it, MARGIN, y, bodyPaint); y += 14f }
            y += 20f
        }

        if (draft.experiences.isNotEmpty()) {
            canvas.drawText("PENGALAMAN", MARGIN, y, sectionTitlePaint); y += 15f
            draft.experiences.forEach { exp ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText("${exp.jobTitle} - ${exp.company}", MARGIN, y, bodyPaint); y += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${exp.startDate} - ${exp.endDate}", MARGIN, y, datePaint); y += 14f
                val descLines = exp.description.chunked(100)
                descLines.forEach { canvas.drawText(it, MARGIN + 10f, y, bodyPaint); y += 14f }
                y += 10f
            }
            y += 10f
        }
        
        if (draft.educations.isNotEmpty()) {
            canvas.drawText("PENDIDIKAN", MARGIN, y, sectionTitlePaint); y += 15f
            draft.educations.forEach { edu ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText(edu.institution, MARGIN, y, bodyPaint); y += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${edu.major} | ${edu.startDate} - ${edu.endDate}", MARGIN, y, datePaint); y += 14f
                val descLines = edu.description.chunked(100)
                descLines.forEach { canvas.drawText(it, MARGIN + 10f, y, bodyPaint); y += 14f }
                y += 10f
            }
        }
    }

    private fun drawCenteredLayout(canvas: Canvas, draft: CvDraft, accentColor: Int, context: Context?, isBold: Boolean) {
        var y = MARGIN
        val centerX = PAGE_WIDTH / 2
        
        val namePaint = Paint().apply { 
            color = if (isBold) accentColor else Color.BLACK
            textSize = if (isBold) 28f else 24f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true 
        }
        val titlePaint = Paint().apply { color = Color.GRAY; textSize = 14f; textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val contactPaint = Paint().apply { color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.CENTER; isAntiAlias = true }

        val baseSize = 80f
        val photoSize = baseSize * draft.personalInfo.profilePhotoScale
        val photo = loadProfilePhoto(context, draft.personalInfo.profilePhotoUri, photoSize, draft.personalInfo.profilePhotoShape)
        if (photo != null) {
            canvas.drawBitmap(photo, centerX - (photoSize / 2), y, null)
            y += photoSize + 20f
        }

        canvas.drawText(draft.personalInfo.fullName.ifEmpty { "Nama Lengkap" }, centerX, y, namePaint); y += 25f
        canvas.drawText(draft.personalInfo.title.ifEmpty { "Jabatan Profesional" }, centerX, y, titlePaint); y += 20f

        val contacts = listOf(draft.personalInfo.email, draft.personalInfo.phone, draft.personalInfo.location).filter { it.isNotBlank() }
        canvas.drawText(contacts.joinToString(" | "), centerX, y, contactPaint); y += 25f

        val sectionTitlePaint = Paint().apply { color = accentColor; textSize = 14f; isFakeBoldText = true; isAntiAlias = true }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 11f; isAntiAlias = true }
        val datePaint = Paint().apply { color = Color.DKGRAY; textSize = 10f; isAntiAlias = true }

        if (draft.personalInfo.summary.isNotEmpty()) {
            canvas.drawText("RINGKASAN", MARGIN, y, sectionTitlePaint); y += 15f
            val lines = draft.personalInfo.summary.chunked(100)
            lines.forEach { canvas.drawText(it, MARGIN, y, bodyPaint); y += 14f }
            y += 20f
        }

        if (draft.experiences.isNotEmpty()) {
            canvas.drawText("PENGALAMAN", MARGIN, y, sectionTitlePaint); y += 15f
            draft.experiences.forEach { exp ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText("${exp.jobTitle} - ${exp.company}", MARGIN, y, bodyPaint); y += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${exp.startDate} - ${exp.endDate}", MARGIN, y, datePaint); y += 14f
                val descLines = exp.description.chunked(100)
                descLines.forEach { canvas.drawText(it, MARGIN + 10f, y, bodyPaint); y += 14f }
                y += 10f
            }
            y += 10f
        }
        
        if (draft.educations.isNotEmpty()) {
            canvas.drawText("PENDIDIKAN", MARGIN, y, sectionTitlePaint); y += 15f
            draft.educations.forEach { edu ->
                bodyPaint.isFakeBoldText = true
                canvas.drawText(edu.institution, MARGIN, y, bodyPaint); y += 14f
                bodyPaint.isFakeBoldText = false
                canvas.drawText("${edu.major} | ${edu.startDate} - ${edu.endDate}", MARGIN, y, datePaint); y += 14f
                val descLines = edu.description.chunked(100)
                descLines.forEach { canvas.drawText(it, MARGIN + 10f, y, bodyPaint); y += 14f }
                y += 10f
            }
        }
    }
}

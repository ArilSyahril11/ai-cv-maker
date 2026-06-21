package com.example.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class GithubRelease(
    val tag_name: String,
    val name: String,
    val assets: List<GithubAsset>
)

data class GithubAsset(
    val name: String,
    val browser_download_url: String
)

class UpdateManager(private val context: Context) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val client = OkHttpClient()

    // Ganti dengan username GitHub yang sesuai (ArilSyahril11)
    private val REPO_OWNER = "ArilSyahril11"
    private val REPO_NAME = "ai-cv-maker"

    suspend fun checkForUpdates(currentVersion: String): GithubRelease? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val adapter = moshi.adapter(GithubRelease::class.java)
                    val release = adapter.fromJson(responseBody)
                    
                    // Simple version comparison (e.g., comparing "v1.0.0" with "v1.0.1")
                    // If tag is different from currentVersion, consider it an update.
                    if (release != null && release.tag_name != currentVersion) {
                        return@withContext release
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun downloadAndInstallUpdate(release: GithubRelease) {
        val apkAsset = release.assets.find { it.name.endsWith(".apk") }
        if (apkAsset == null) {
            Toast.makeText(context, "File APK tidak ditemukan pada rilis ini", Toast.LENGTH_SHORT).show()
            return
        }

        val url = apkAsset.browser_download_url
        val fileName = apkAsset.name
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Delete old file if exists
        if (destination.exists()) {
            destination.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Mengunduh Pembaruan AI CV Maker")
            .setDescription("Versi ${release.tag_name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        Toast.makeText(context, "Mulai mengunduh...", Toast.LENGTH_SHORT).show()

        // Register BroadcastReceiver to detect when download finishes
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuka installer", Toast.LENGTH_SHORT).show()
        }
    }
}

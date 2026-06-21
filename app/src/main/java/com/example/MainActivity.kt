package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.network.GeminiHelper
import com.example.ui.theme.MyApplicationTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var webViewRef: WebView? = null
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                // Enable standard features in local asset container
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                }
                                
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()

                                // Set up Javascript Bridge Interface
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun getStorageItem(key: String): String? {
                                            val sharedPref = context.getSharedPreferences("cv_maker_prefs", Context.MODE_PRIVATE)
                                            return sharedPref.getString(key, null)
                                        }

                                        @JavascriptInterface
                                        fun setStorageItem(key: String, value: String) {
                                            val sharedPref = context.getSharedPreferences("cv_maker_prefs", Context.MODE_PRIVATE)
                                            with(sharedPref.edit()) {
                                                putString(key, value)
                                                apply()
                                            }
                                        }

                                        @JavascriptInterface
                                        fun triggerAiPolish(type: String, text: String, callbackId: String) {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                try {
                                                    val geminiHelper = GeminiHelper()
                                                    val result = geminiHelper.polishText(type, text)
                                                    
                                                    runOnUiThread {
                                                        val jsonString = moshi.adapter(String::class.java).toJson(result)
                                                        webViewRef?.evaluateJavascript(
                                                            "window.onAiPolishResult('$callbackId', true, $jsonString)",
                                                            null
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    val errMessage = "Error: " + (e.localizedMessage ?: e.message ?: "Unknown error")
                                                    runOnUiThread {
                                                        val jsonString = moshi.adapter(String::class.java).toJson(errMessage)
                                                        webViewRef?.evaluateJavascript(
                                                            "window.onAiPolishResult('$callbackId', false, $jsonString)",
                                                            null
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        @JavascriptInterface
                                        fun sharePdf(base64Data: String, fileName: String) {
                                            runOnUiThread {
                                                saveAndSharePdf(this@MainActivity, base64Data, fileName)
                                            }
                                        }
                                    },
                                    "AndroidBridge"
                                )

                                loadUrl("file:///android_asset/index.html")
                                webViewRef = this
                            }
                        }
                    )
                }
            }
        }
    }

    private fun saveAndSharePdf(context: Context, base64Data: String, fileName: String) {
        try {
            val pdfBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(pdfBytes)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan CV PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}


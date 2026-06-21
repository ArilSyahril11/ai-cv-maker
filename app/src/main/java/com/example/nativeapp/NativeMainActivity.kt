package com.example.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nativeapp.ui.DashboardScreen
import com.example.nativeapp.viewmodel.CvViewModel
import com.example.ui.theme.MyApplicationTheme

class NativeMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NativeAppNavigation()
                }
            }
        }
    }
}

@Composable
fun NativeAppNavigation(viewModel: CvViewModel = viewModel()) {
    val navController = rememberNavController()
    val drafts by viewModel.allDrafts.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var draftToExport by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.model.CVData?>(null) }
    
    val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()
    val jsonAdapter = moshi.adapter(com.example.model.CVData::class.java)

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && draftToExport != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    val json = jsonAdapter.toJson(draftToExport)
                    os.write(json.toByteArray())
                }
                android.widget.Toast.makeText(context, "Proyek .cvm berhasil diekspor", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Gagal mengekspor: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val jsonString = input.bufferedReader().readText()
                    
                    var modifiedJson = jsonString
                    try {
                        val jsonObj = org.json.JSONObject(jsonString)
                        // Periksa apakah ini format .cvm lama (punya "basic" tapi tidak punya "id")
                        if (jsonObj.has("basic") && !jsonObj.has("id")) {
                            val basic = jsonObj.optJSONObject("basic") ?: org.json.JSONObject()
                            jsonObj.put("id", "cv_" + System.currentTimeMillis())
                            val name = basic.optString("fullName", "")
                            jsonObj.put("title", if (name.isNotEmpty()) "CV $name" else "Proyek Impor Lama")
                            jsonObj.put("lastModified", System.currentTimeMillis())
                            jsonObj.put("template", "executive_classic")
                            jsonObj.put("accentColor", "#06b6d4")
                            jsonObj.put("personal", basic)
                            modifiedJson = jsonObj.toString()
                        }
                    } catch (e: Exception) {
                        // Abaikan error di sini, biarkan Moshi yang menangkap error format
                    }

                    val data = jsonAdapter.fromJson(modifiedJson)
                    if (data != null) {
                        viewModel.importDraft(data)
                        android.widget.Toast.makeText(context, "Proyek .cvm berhasil diimpor", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Format file tidak valid: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                drafts = drafts,
                onCreateNew = { viewModel.createNewDraft() },
                onOpenDraft = { draftId ->
                    navController.navigate("editor/$draftId")
                },
                onDeleteDraft = { draft ->
                    viewModel.deleteDraft(draft)
                },
                onGenerateDummy = { viewModel.generateDummyCv() },
                onImportProject = { importLauncher.launch(arrayOf("*/*")) },
                onExportDraft = { draft ->
                    draftToExport = draft
                    val cleanTitle = draft.title.ifEmpty { "Proyek_CV" }.replace("\\s+".toRegex(), "_")
                    exportLauncher.launch("$cleanTitle.cvm")
                }
            )
        }
        composable("editor/{draftId}") { backStackEntry ->
            val draftId = backStackEntry.arguments?.getString("draftId") ?: return@composable
            val draft = drafts.find { it.id == draftId }
            
            if (draft != null) {
                com.example.nativeapp.ui.EditorScreen(
                    context = androidx.compose.ui.platform.LocalContext.current,
                    draft = draft,
                    onBack = { navController.popBackStack() },
                    onSave = { updatedDraft ->
                        viewModel.updateDraft(updatedDraft)
                        navController.popBackStack()
                    }
                )
            } else {
                androidx.compose.material3.Text("Draft tidak ditemukan.")
            }
        }
    }
}

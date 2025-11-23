package at.sljivic.aegis

import SettingsViewModel
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import at.sljivic.aegis.MyActivityHolder.currentActivity
import at.sljivic.aegis.logic.setting.SettingsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private var filePickerContinuation: (String?) -> Unit = {}

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            filePickerContinuation(uri?.toString())
        }

    suspend fun pickFileFromActivity(): String? = suspendCancellableCoroutine { cont ->
        filePickerContinuation = { result -> cont.resume(result) }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        filePickerLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repo = SettingsRepository()
        val viewModel = SettingsViewModel(repo, lifecycleScope)

        currentActivity = this

        setContent {
            App(viewModel)
        }
    }
}
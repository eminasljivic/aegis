package at.sljivic.aegis

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class AndroidFilePicker(
    private val activity: ComponentActivity,
    private val onFilePicked: (Uri) -> Unit
) : FilePicker {

    private val launcher =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                activity.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onFilePicked(it)
            }
        }

    override fun pickFile() {
        launcher.launch(arrayOf("*/*"))
    }
}
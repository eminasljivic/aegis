package at.sljivic.aegis.filePicker

import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.FileDialog
import java.awt.Frame
import kotlin.coroutines.resume

actual fun provideFilePicker(): FilePicker = object : FilePicker {
    override suspend fun pickFile(): String? = suspendCancellableCoroutine { cont ->
        val dialog = FileDialog(null as Frame?, "Select a File", FileDialog.LOAD)
        dialog.isVisible = true

        if (dialog.file != null) {
            val path = dialog.directory + dialog.file
            cont.resume(path)
        }
    }
}
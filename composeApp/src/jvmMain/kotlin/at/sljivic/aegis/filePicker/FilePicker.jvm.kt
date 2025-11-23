package at.sljivic.aegis.filePicker

import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.FileDialog
import java.awt.Frame
import kotlin.coroutines.resume

actual fun provideFilePicker(): FilePicker = object : FilePicker {
    override suspend fun pickFile(): String? = suspendCancellableCoroutine { cont ->
        val dialog = FileDialog(Frame(), "Select", FileDialog.LOAD)

        // Run the dialog on the EDT
        java.awt.EventQueue.invokeLater {
            dialog.isVisible = true
            val result = if (dialog.file != null) {
                dialog.directory + dialog.file
            } else {
                null
            }
            cont.resume(result)
        }
    }
}

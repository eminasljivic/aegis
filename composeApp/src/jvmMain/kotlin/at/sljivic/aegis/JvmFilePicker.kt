package at.sljivic.aegis

import java.awt.FileDialog
import java.awt.Frame


class JvmFilePicker(
    private val onFilePicked: (String) -> Unit
) : FilePicker {

    override fun pickFile() {
        val dialog = FileDialog(null as Frame?, "Select a File", FileDialog.LOAD)
        dialog.isVisible = true

        if (dialog.file != null) {
            val path = dialog.directory + dialog.file
            onFilePicked(path)
        }
    }
}
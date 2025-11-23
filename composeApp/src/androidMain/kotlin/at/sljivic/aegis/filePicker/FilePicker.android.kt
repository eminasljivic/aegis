package at.sljivic.aegis.filePicker

import at.sljivic.aegis.MainActivity
import at.sljivic.aegis.MyActivityHolder

actual fun provideFilePicker(): FilePicker =
    object : FilePicker {
        override suspend fun pickFile(): String? {
            val activity = MyActivityHolder.currentActivity as? MainActivity ?: return null

            return activity.pickFileFromActivity()
        }
    }
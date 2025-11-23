package at.sljivic.aegis.filePicker

interface FilePicker {
    suspend fun pickFile(): String?
}

expect fun provideFilePicker(): FilePicker
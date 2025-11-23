package at.sljivic.aegis.filePicker

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
typealias OnFilePicked = suspend (filePath: String) -> Unit

@Composable
fun FilePickerButton(
    filePicker: FilePicker,
    onFilePicked: OnFilePicked
) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            val filePath = filePicker.pickFile()
            filePath?.let {
                val fileName = it.substringAfterLast('/') // extract name
                onFilePicked(fileName) // call common logic
            }
        }
    }) {
        Text("Pick a File")
    }
}
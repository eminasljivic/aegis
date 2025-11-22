package at.sljivic.aegis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(filePicker: FilePicker) {
    AppTheme {
        Column(
                modifier =
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                .safeContentPadding()
                                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val file_only_policy =
                    ArrayList<OperationType>(
                            listOf(OperationType.Network, OperationType.ProcessManagement)
                    )
            createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")
            getSyscallList()
            Button(onClick = { filePicker.pickFile() }) { Text("Select File") }
        }
    }
}

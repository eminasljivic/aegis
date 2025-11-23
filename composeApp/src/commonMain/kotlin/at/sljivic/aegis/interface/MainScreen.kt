package at.sljivic.aegis.`interface`

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.sljivic.aegis.core.theme.AppTheme
import at.sljivic.aegis.filePicker.FilePickerButton
import at.sljivic.aegis.filePicker.provideFilePicker
import at.sljivic.aegis.logic.setting.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    settingsRepository: SettingsRepository
) {
    var darkMode by remember { mutableStateOf(settingsRepository.isDarkMode()) }
    var selectedFile by remember { mutableStateOf<String?>(null) }

    AppTheme(darkTheme = darkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("aegis", color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FilePickerButton(
                    text = "Select an Executable",
                    filePicker = provideFilePicker(),
                ) { fileName ->
                    selectedFile = fileName
                }

                selectedFile?.let { file ->
                    LogScreen(file, settingsRepository.getArgs(), settingsRepository.getPolicyFile())
                }
            }
        }
    }
}

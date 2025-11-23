package at.sljivic.aegis.`interface`

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import at.sljivic.aegis.filePicker.FilePickerButton
import at.sljivic.aegis.filePicker.provideFilePicker
import at.sljivic.aegis.logic.Syscall
import at.sljivic.aegis.logic.getSyscallList
import at.sljivic.aegis.logic.setting.SettingsRepository
import com.example.compose.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onSettingsClick: () -> Unit
               settingsRepository: SettingsRepository
) {
    var darkMode by remember { mutableStateOf(settingsRepository.isDarkMode()) }
    var syscalls by remember { mutableStateOf<List<Syscall>>(emptyList()) }

    AppTheme(darkTheme = darkMode) {
        Scaffold(
                topBar = {
                    TopAppBar(
                            title = { Text("Main Page") },
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
                        filePicker = provideFilePicker(),
                ) { fileName ->
                    syscalls = getSyscallList(fileName) // ← update state
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(syscalls) { sc -> Text(sc.toString()) }
                }
            }
        }
    }
}

package at.sljivic.aegis

import SettingsViewModel
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.sljivic.aegis.logic.setting.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File

fun main() = application {
    val repo = SettingsRepository(File(System.getProperty("user.home") + "/app_settings.json"))
    val viewModel = SettingsViewModel(repo, CoroutineScope(Dispatchers.Main))

    Window(
        onCloseRequest = ::exitApplication,
        title = "aegis",
    ) { App(viewModel) }
}

package at.sljivic.aegis

import SettingsViewModel
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.sljivic.aegis.logic.setting.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun main() = application {
    val repo = SettingsRepository()
    val viewModel = SettingsViewModel(repo, CoroutineScope(Dispatchers.Main))

    Window(
        onCloseRequest = ::exitApplication,
        title = "aegis",
    ) { App(viewModel) }
}

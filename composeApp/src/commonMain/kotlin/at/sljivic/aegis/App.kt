package at.sljivic.aegis


import SettingsViewModel
import androidx.compose.runtime.*
import at.sljivic.aegis.`interface`.MainScreen
import at.sljivic.aegis.`interface`.setting.SettingsScreen
import org.jetbrains.compose.ui.tooling.preview.Preview


enum class Screen {
    MAIN,
    SETTINGS
}

@Composable
@Preview
fun App(settingsViewModel: SettingsViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    when (currentScreen) {
        Screen.MAIN -> MainScreen(
            onSettingsClick = { currentScreen = Screen.SETTINGS }
        )

        Screen.SETTINGS -> SettingsScreen(
            viewModel = settingsViewModel,
            onBack = { currentScreen = Screen.MAIN }
        )
    }
}
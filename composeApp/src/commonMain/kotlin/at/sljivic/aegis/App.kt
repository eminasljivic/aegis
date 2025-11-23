package at.sljivic.aegis


import SettingsViewModel
import androidx.compose.runtime.*
import at.sljivic.aegis.`interface`.MainScreen
import at.sljivic.aegis.`interface`.setting.SettingsScreen
import at.sljivic.aegis.logic.Syscall
import org.jetbrains.compose.ui.tooling.preview.Preview


fun getTraceStrings(Syscalls: ArrayList<Syscall>): List<String> {
    val list = ArrayList<String>()
    for (sys in Syscalls) {
        list.add("Tracer OUT: ${sys}")
    }
    return list
}

enum class Screen {
    MAIN,
    SETTINGS
}

@Composable
@Preview
fun App(settingsVieModel: SettingsViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.MAIN) }

    when (currentScreen) {
        Screen.MAIN -> MainScreen(
            onSettingsClick = { currentScreen = Screen.SETTINGS }
        )
        Screen.SETTINGS -> SettingsScreen(viewModel = settingsVieModel)
    }
}
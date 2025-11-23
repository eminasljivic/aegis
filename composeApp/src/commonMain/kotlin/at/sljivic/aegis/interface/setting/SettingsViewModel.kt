import at.sljivic.aegis.logic.setting.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(
    val repo: SettingsRepository,
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _darkMode = MutableStateFlow(repo.isDarkMode())
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _sandbox = MutableStateFlow(repo.isSandboxMode())
    val sandbox: StateFlow<Boolean> = _sandbox

    fun toggleDarkMode() {
        scope.launch {
            val newValue = !_darkMode.value
            repo.setDarkMode(newValue)
            _darkMode.value = newValue
        }
    }

    fun toggleSandbox() {
        scope.launch {
            val newValue = !_sandbox.value
            repo.setSandboxMode(newValue)
            _sandbox.value = newValue
        }
    }
}

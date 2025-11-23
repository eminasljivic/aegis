import at.sljivic.aegis.logic.setting.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
        val repo: SettingsRepository,
        val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _darkMode = MutableStateFlow(repo.isDarkMode())
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _sandbox = MutableStateFlow(repo.isSandboxMode())
    val sandbox: StateFlow<Boolean> = _sandbox

    private val _args = MutableStateFlow(repo.getArgs())
    val args: StateFlow<String> = _args

    private val _policyFile = MutableStateFlow(repo.getArgs())
    val policyFile: StateFlow<String> = _policyFile

    fun toggleDarkMode() {
        scope.launch {
            val newValue = !_darkMode.value
            repo.setDarkMode(newValue)
            _darkMode.value = newValue
        }
    }

    fun setArgs(value: String) {
        scope.launch {
            repo.setArgs(value)
            _args.value = value
        }
    }

    fun setPolicyFile(value: String) {
        scope.launch {
            repo.setPolicyFile(value)
            _policyFile.value = value
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

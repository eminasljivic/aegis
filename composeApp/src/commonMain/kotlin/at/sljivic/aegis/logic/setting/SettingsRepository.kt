package at.sljivic.aegis.logic.setting

import kotlinx.serialization.json.Json
import java.io.File


class SettingsRepository(
    private val file: File
) {
    private val json = Json { prettyPrint = true }
    private var cachedSettings: AppSettings? = null

    fun load(): AppSettings {
        return cachedSettings ?: if (file.exists()) {
            json.decodeFromString<AppSettings>(file.readText()).also { cachedSettings = it }
        } else {
            AppSettings().also { cachedSettings = it }
        }
    }

    fun save(settings: AppSettings) {
        file.writeText(json.encodeToString(settings))
        cachedSettings = settings
    }

    fun isDarkMode() = load().darkMode
    fun isSandboxMode() = load().sandboxMode
    fun getArgs() = load().args

    fun setDarkMode(enabled: Boolean) {
        val current = load()
        save(current.copy(darkMode = enabled))
    }

    fun setSandboxMode(enabled: Boolean) {
        val current = load()
        save(current.copy(sandboxMode = enabled))
    }

    fun setArgs(newargs: String) {
        var current = load()
        save(current.copy(args = newargs))
    }
}
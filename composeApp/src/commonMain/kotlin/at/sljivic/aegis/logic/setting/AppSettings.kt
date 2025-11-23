package at.sljivic.aegis.logic.setting

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val darkMode: Boolean = false,
    val sandboxMode: Boolean = false
)
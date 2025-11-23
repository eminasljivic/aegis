package at.sljivic.aegis.`interface`.setting

import SettingsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.sljivic.aegis.core.theme.AppTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: (() -> Unit)? = null) {
    val darkMode by viewModel.darkMode.collectAsState()
    val sandbox by viewModel.sandbox.collectAsState()
    val args by viewModel.args.collectAsState()
    val policy by viewModel.policyFile.collectAsState()

    AppTheme(darkTheme = darkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // <-- Set the background
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Text("Settings", color = MaterialTheme.colorScheme.onBackground)
            }

            SettingsToggle(
                title = "Dark Mode",
                value = darkMode,
                onToggle = { viewModel.toggleDarkMode() }
            )

            SettingsToggle(
                title = "Sandbox Mode",
                value = sandbox,
                onToggle = { viewModel.toggleSandbox() }
            )

            TextField(
                value = args,
                onValueChange = { viewModel.setArgs(it) },
                label = { Text("Arguments") }
            )

            TextField(
                value = policy,
                onValueChange = { viewModel.setPolicyFile(it) },
                label = { Text("Policy path") },
            )
        }
    }
}

@Composable
fun SettingsToggle(title: String, value: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = value, onCheckedChange = { onToggle() })
    }
}

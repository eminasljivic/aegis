package at.sljivic.aegis.`interface`

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.sljivic.aegis.logic.TraceEvent
import at.sljivic.aegis.logic.runTrace

@Composable
fun TraceTerminal(path: String) {
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }

    val appOutErr = lines.filter { it.startsWith("APP") || it.startsWith("APP ERR") }
    val syscallEvents = lines.filter { it.startsWith("SYS") || it.startsWith("TRACER ERR") || it.startsWith("---- FINISHED ----") }

    // Keep your existing LaunchedEffect exactly
    LaunchedEffect(path) {
        runTrace(path).collect { event ->
            val line = when (event) {
                is TraceEvent.SyscallEvent -> "SYS: ${event.name}"
                is TraceEvent.TracerErr    -> ""
                is TraceEvent.AppOut       -> "${event.msg}"
                is TraceEvent.AppErr       -> "APP STDERR: ${event.msg}"
                TraceEvent.Finished        -> "---- FINISHED ----"
            }
            lines = lines + line
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // App Output / Errors
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = "=== App Output / Errors ==="
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(appOutErr) { line ->
                    Text(line, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Divider(color = Color.Gray, thickness = 1.dp)

        // Syscall Events
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = "=== Syscall Events ===",
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(syscallEvents) { line ->
                    Text(line, modifier = Modifier.fillMaxWidth(), color = Color.Red)
                }
            }
        }
    }
}
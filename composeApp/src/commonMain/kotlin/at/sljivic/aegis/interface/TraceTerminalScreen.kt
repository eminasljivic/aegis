package at.sljivic.aegis.`interface`

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import at.sljivic.aegis.logic.TraceEvent
import at.sljivic.aegis.logic.runTrace

data class LogEntry(val type: LogType, val message: String)
enum class LogType {
    APP_OUT,
    APP_ERR,
    SYSCALL,
    TRACER_ERR,
    FINISHED
}

@Composable
fun LogScreen(path: String, args: String, policyFile: String) {
    val app = remember { mutableStateListOf<LogEntry>() }
    val syscallEvents = remember { mutableStateListOf<LogEntry>() }

    LaunchedEffect(path, args) {
        runTrace(path, args, policyFile).collect { event ->
            when (event) {
                is TraceEvent.SyscallEvent -> {
                    syscallEvents.add(LogEntry(LogType.SYSCALL, "${event.name.name} of type ${event.name.type}"))
                    println(syscallEvents)
                }
                is TraceEvent.AppOut -> {
                    app.add(LogEntry(LogType.APP_OUT, event.msg))
                }
                is TraceEvent.AppErr -> {
                    app.add(LogEntry(LogType.APP_ERR, event.msg))
                }
                TraceEvent.Finished -> {
                    syscallEvents.add(LogEntry(LogType.FINISHED, "---- FINISHED ----"))
                }
                else -> {}
            }
        }
    }

    Column(modifier = Modifier
        .background(MaterialTheme.colorScheme.background) // use colorScheme, not colors
        .padding(8.dp)
    ) {
        // App Output / Errors
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "=== App Output / Errors ===",
                color = MaterialTheme.colorScheme.primary
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(app) { line ->
                    Text(
                        line.message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        fontFamily = FontFamily.Monospace,
                        color = if (line.type == LogType.APP_ERR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Syscall Events
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(
                text = "=== Syscall Events ===",
                color = MaterialTheme.colorScheme.primary
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(syscallEvents) { line ->
                    Text(
                        line.message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}
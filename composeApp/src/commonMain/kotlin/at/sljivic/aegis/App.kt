package at.sljivic.aegis


import java.io.BufferedReader
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import at.sljivic.aegis.PolicyRule
import at.sljivic.aegis.syscallNumToSyscall
import at.sljivic.aegis.getSyscallList
import at.sljivic.aegis.getSyscallListMock
import kotlin.concurrent.thread
import kotlin.math.sign

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compose.darkBackground
import com.example.compose.darkForeground
import com.example.compose.lightBackground
import com.example.compose.lightForeground
import com.example.compose.AppTheme
import kotlinx.coroutines.delay




fun getSyscallListMock(): ArrayList<Syscall> {
    println("=== MOCK MODUS ===")

    // Simuliertes TracingResult
    val mockBuffer = StreamingBuffer()

    // Füge verschiedene Syscalls hinzu (simulierte Ausgabe)
    val mockSyscalls = listOf(12, 3, 22, 2, 1, 60, 9, 11, 158, 231, 12, 3, 22, 2, 1, 60, 9, 11, 158, 231, 12, 3, 22, 2, 1, 60, 9, 11, 158, 231, 12, 3, 22, 2, 1, 60, 9, 11, 158, 231)
    mockSyscalls.forEach { syscallNum ->
        mockBuffer.append(syscallNum.toString())
    }

    val traceResult = TracingResult(
        tracerOut = mockBuffer,
        tracerErr = StreamingBuffer(),
        appOut = StreamingBuffer(),
        appErr = StreamingBuffer(),
        tracerOutThread = Thread(),
        tracerErrThread = Thread(),
        appOutThread = Thread(),
        appErrThread = Thread(),
        tracerProc = createMockProcess()
    )

    val syscalls_in_order = ArrayList<Syscall>()
    var last_time = false

    while (true) {
        val outChunk = traceResult.tracerOut.drain()
        if (outChunk.isNotEmpty() && outChunk.isNotBlank()) {
            val outs = outChunk.split("\n")
            for(out_syscall in outs) {
                if(out_syscall.isEmpty() || out_syscall.isBlank()) continue
                try {
                    val name = syscallNumToSyscall(out_syscall.toInt())
                    syscalls_in_order.add(name)
                    println("MOCK Tracer OUT: $name")
                } catch (e: Exception) {
                    println("MOCK Fehler bei Syscall: $out_syscall")
                }
            }
        }

        // Simuliere dass Prozess nach 2 Iterationen "stirbt"
        if (!last_time) {
            last_time = true
        } else {
            break
        }

        Thread.sleep(100)
    }

    println("MOCK: Gefundene Syscalls: ${syscalls_in_order.size}")
    return syscalls_in_order
}

fun createMockProcess(): Process {
    return ProcessBuilder("echo", "mock").start().apply {
        waitFor() // Prozess sofort beenden
    }
}

fun getTraceStrings(Syscalls: ArrayList<Syscall>): List<String> {
    val list = ArrayList<String>()
    for(sys in Syscalls) {
        list.add("name: ${sys.name}   || type: ${sys.type}")
    }
    return list
}



@Composable
@Preview
fun App(filePicker: FilePicker) {

    val showMainStats = remember { mutableStateOf(false) }
    val isDarkMode = remember { mutableStateOf(true) } 


    AppTheme (darkTheme = isDarkMode.value){
        Column(
            modifier =
                Modifier.background(if (isDarkMode.value)  darkBackground else lightBackground)
                    .safeContentPadding()
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Dark Mode Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Switch(
                    checked = isDarkMode.value,
                    onCheckedChange = { isDarkMode.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = darkForeground,
                        checkedTrackColor = darkForeground.copy(alpha = 0.5f) ,
                        uncheckedThumbColor = darkForeground,
                        uncheckedTrackColor = lightForeground
                    )
                )
                Text(
                    "Dark Mode",
                    color = if (isDarkMode.value) darkForeground else lightForeground,
                    modifier = Modifier.padding(end = 8.dp)
                )

            }
            val file_only_policy = ArrayList<OperationType>(listOf(OperationType.Network, OperationType.ProcessManagement));
            createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")

            Button(
                onClick = { filePicker.pickFile()
                    showMainStats.value = true
                }
                ,
            ) {
                Text("Select File")
            }



            if(showMainStats.value) {


                // trace file
                val traceResult = getSyscallListMock()

                var terminalLines by remember { mutableStateOf(listOf("Starting...")) }

                LaunchedEffect(traceResult) {
                    val strings = getTraceStrings(traceResult)
                    for (s in strings) {
                        terminalLines = terminalLines + s
                        delay(100)
                    }
                }

                Row(Modifier.fillMaxSize()) {

                    // Left: "terminal"
                    TerminalPane(
                        lines = terminalLines,
                        modifier = Modifier
                        .heightIn(max = 300.dp)
                        .widthIn(max = 500.dp)
                        .offset(x = 320.dp, y = 220.dp)
                    )

                }


                Button(
                    onClick = { },
                ) {
                    Text("Stop output")
                }
            }

        }
    }

}


@Composable
fun TerminalPane(
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.scrollToItem(lines.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(lines) { line ->
            Text(
                line,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }
    }
}


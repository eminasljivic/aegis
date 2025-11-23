package at.sljivic.aegis


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
import com.example.compose.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview


fun getTraceStrings(Syscalls: ArrayList<Syscall>): List<String> {
    val list = ArrayList<String>()
    for(sys in Syscalls) {
        list.add("Tracer OUT: ${sys}")
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
            // val file_only_policy = ArrayList<OperationType>(listOf(OperationType.Network, OperationType.ProcessManagement));
            // createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")

            Button(
                onClick = { filePicker.pickFile()
                    showMainStats.value = true
                }
                ,
            ) {
                Text("Select File")
            }



            if(showMainStats.value) {

                var path = "/bin/ls"
                // trace file
                val traceResult = getSyscallList(path)

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
                        filePicker,
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
    filePicker: FilePicker,
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
            val file_only_policy =
                    ArrayList<OperationType>(
                            listOf(OperationType.Network, OperationType.ProcessManagement)
                    )
            // createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")

            var path = "/bin/ls"
            Button(onClick = { filePicker.pickFile() }) { Text("Select File") }
            Button(
                    onClick = {
                        println("Path to exe: $path")
                        getSyscallList(path)
                    },
            ) { Text("Run!") }
        }
    }
}


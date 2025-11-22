package at.sljivic.aegis




import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import java.io.BufferedReader
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import at.sljivic.aegis.PolicyRule
import at.sljivic.aegis.syscallNumToSyscall
import at.sljivic.aegis.getSyscallList
import at.sljivic.aegis.getSyscallListMock
import kotlin.concurrent.thread

fun getName(traceResult: TracingResult): String{
    val outChunk = traceResult.tracerOut.drain()
        if (outChunk.isNotEmpty() && outChunk.isNotBlank()) {
            val outs = outChunk.split("\n")
            for(out_syscall in outs) {
                if(out_syscall.isEmpty() || out_syscall.isBlank() ) continue
            val name = syscallNumToSyscall(out_syscall.toInt())
            // syscalls_in_order.add(name)
            return "$name"
            }
        }
        return "" 
    
}

fun getSyscallListMock(): TracingResult {
    
    // Simuliertes TracingResult
    val mockBuffer = StreamingBuffer()
    
    // Füge verschiedene Syscalls hinzu (simulierte Ausgabe)
    val mockSyscalls = listOf(12, 3, 22, 2, 1, 60, 9, 11, 158, 231)
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

    return traceResult
    
}

fun createMockProcess(): Process {
    return ProcessBuilder("echo", "mock").start().apply {
        waitFor() // Prozess sofort beenden
    }
}



@Composable
@Preview
fun App(filePicker: FilePicker) {

    val showSecondButton = remember { mutableStateOf(false) }
    

    AppTheme {
        Column(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val file_only_policy = ArrayList<OperationType>(listOf(OperationType.Network, OperationType.ProcessManagement));

            Button(
                onClick = { createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")
                showSecondButton.value = true}, 
            ) {
                Text("Create Policy")
            }

            if(showSecondButton.value) {
                Button(
                    onClick = { filePicker.pickFile() },
                ) {
                    Text("Select File")
                }
            }
            


           // getSyscallList();
           val traceResult = getSyscallListMock()

           Text(getName(traceResult))
           
            Button(
                onClick = { },
            ) {
                Text("Stop output")
            }
        }
    }

}

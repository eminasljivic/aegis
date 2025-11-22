package at.sljivic.aegis

import aegis.composeapp.generated.resources.Res
import aegis.composeapp.generated.resources.compose_multiplatform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class OperationType {
    File,
    Network,
    ProcessManagement,
    Unclassified
}

data class Syscall(val name: String, val type: OperationType)

expect fun syscallNumToSyscall(num: Int): Syscall

expect fun traceExecutable(executablePath: String, args: List<String>, timeoutSeconds: Long): String

fun getSyscallList(): ArrayList<Syscall> {
    val nums = traceExecutable("ls", listOf("."), 60).split("\n")
    var syscalls_in_order = ArrayList<Syscall>()
    for (num in nums) {
        //   println("to int: $num");
        val intNum = num.toInt()
        val syscall = syscallNumToSyscall(intNum)
        // println("Syscall was: $syscall")
        syscalls_in_order.add(syscall)
    }
    return syscalls_in_order
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
                modifier =
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                .safeContentPadding()
                                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) { Text("Click me!") }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                println(getSyscallList())
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

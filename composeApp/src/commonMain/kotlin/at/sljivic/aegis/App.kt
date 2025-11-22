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
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.BufferedReader
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import at.sljivic.aegis.PolicyRule
import at.sljivic.aegis.syscallNumToSyscall
import at.sljivic.aegis.getSyscallList
import kotlin.concurrent.thread


data class SandboxingOptions(val syscall_restrictions: ArrayList<Int>)

enum class OperationType {
    File,
    Network,
    ProcessManagement,
    Unclassified
}

data class Syscall(val name: String, val type: OperationType)

expect fun syscallNumToSyscall(num: Int): Syscall
expect fun syscallNameToNum(name: String): Int
expect fun getNumSyscalls(): Int
expect fun getSyscallsOfType(type: OperationType): ArrayList<String>

class StreamingBuffer {
    private val sb = StringBuilder()

    @Synchronized
    fun append(line: String) {
        sb.append(line).append('\n')
    }

    @Synchronized
    fun drain(): String {
        if (sb.isEmpty()) return ""
        val out = sb.toString()
        sb.setLength(0)  // clear
        return out
    }
}

data class TracingResult (
    val tracerOut: StreamingBuffer,
    val tracerErr: StreamingBuffer,
    val appOut: StreamingBuffer,
    val appErr: StreamingBuffer,
    val tracerOutThread: Thread,
    val tracerErrThread: Thread,
    val appOutThread: Thread,
    val appErrThread: Thread,
    val tracerProc: Process
)

expect fun traceExecutable(
        executablePath: String,
        args: List<String>,
        timeoutSeconds: Long,
        sandbox: SandboxingOptions
): TracingResult




enum class Action {
    Allow, Deny
}

data class PolicyRule(val action: Action, val syscall_name: String)

fun parsePolicyFile(path: String): SandboxingOptions {
    val bufferedReader: BufferedReader = File(path).bufferedReader()
    val policyContent = bufferedReader.use { it.readText() }
    val policyLines = policyContent.split("\n");

    val rules = ArrayList<PolicyRule>();
    println("Rule file:");
    println(policyContent);
    println("===========\n")

    for (rule in policyLines) {
        if (rule == "" || rule.isBlank()) {
            continue
        }
        println("Looking at rule");
        println(rule);

        val parts = rule.split(" ")
        if (parts.size != 2) {
            println("Rule file malformed. Part size is not 2")
        }
        if (parts.get(0) == "ALLOW")
            rules.add(PolicyRule(Action.Allow, parts.get(1)));
        else if (parts.get(0) == "DENY")
            rules.add(PolicyRule(Action.Deny, parts.get(1)));
        else
            println("Rule file malformed. Action neither allow nor deny")
    }
    if (rules.get(0).syscall_name != "DEFAULT") {
        println("Rule file malformed. No default action")
    }
    val sandbox = SandboxingOptions(ArrayList<Int>());
    if (rules.get(0).action == Action.Allow) {
        // the rest is all denies - TODO check explciitly
        for (rule in rules) {
            if (rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                sandbox.syscall_restrictions.add(syscallNameToNum(rule.syscall_name));
            }
        }
    } else {
        // we need to deny all not explicitly mentioned
        // assume the numbers are sorted for now
        var curr_num = 0
        for (rule in rules) {
            if (rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                val policy_num = syscallNameToNum(rule.syscall_name)
                while (curr_num != policy_num && curr_num < getNumSyscalls()) {
                    sandbox.syscall_restrictions.add(curr_num);
                    curr_num++;
                }
                if (curr_num == policy_num) {
                    curr_num++;
                }
            }
        }
    }


    return sandbox;
}

fun createPolicy(restrictions: ArrayList<OperationType>, path: String) {
    var output_policy = "ALLOW DEFAULT";
    for (restriction in restrictions) {
        var syscall_names = getSyscallsOfType(restriction);
        for (syscall in syscall_names) {
            output_policy += "\nDENY " + syscall;
        }
    }
    File(path).writeText(output_policy);
}


fun getSyscallList(): ArrayList<Syscall> {
 //   val sandbox_config = parsePolicyFile("/tmp/HackaTUM/policy.aegis");
    val sandbox_config = parsePolicyFile("/tmp/HackaTUM/gen_policy.aegis");
    var syscalls_in_order = ArrayList<Syscall>()
    val traceResult =
            traceExecutable("id", listOf(), 60, sandbox_config)
            // we trust it will die at some point
            var last_time =false;
   while (true) {
        val outChunk = traceResult.tracerOut.drain()
        if (outChunk.isNotEmpty() && outChunk.isNotBlank()) {
            val outs = outChunk.split("\n")
            for(out_syscall in outs) {
                if(out_syscall.isEmpty() || out_syscall.isBlank() ) continue
            val name = syscallNumToSyscall(out_syscall.toInt())
             syscalls_in_order.add(name)
            println("Tracer OUT: $name")
            }
        }

        val errChunk = traceResult.tracerErr.drain()
        if (errChunk.isNotEmpty() && errChunk.isNotBlank()) {
            println("Tracer ERR: $errChunk")
        }

        val appOutChunk = traceResult.appOut.drain()
        if (appOutChunk.isNotEmpty() && appOutChunk.isNotBlank()) {
             println("Tracer ERR: $appOutChunk")
        }

        val appErrChunk = traceResult.appErr.drain()
        if (appErrChunk.isNotEmpty() && appErrChunk.isNotBlank()) {
            println("Tracer ERR: $appErrChunk")
        }

    if(last_time) break

    if(!traceResult.tracerProc.isAlive())
    {
        last_time =true;
        traceResult.tracerOutThread.join()
        traceResult.tracerErrThread.join()
        traceResult.appErrThread.join()
        traceResult.appOutThread.join()
    }
        Thread.sleep(100)
    }


    return syscalls_in_order
}


@Composable
@Preview
fun App(filePicker: FilePicker) {
    AppTheme {
        Column(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val file_only_policy = ArrayList<OperationType>(listOf(OperationType.Network, OperationType.ProcessManagement));
            createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")
            getSyscallList();

            Button(onClick = { filePicker.pickFile() }) { Text("Select File") }
        }
    }

}

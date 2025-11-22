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
import kotlin.concurrent.thread



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
             println("app out: $appOutChunk")
        }

        val appErrChunk = traceResult.appErr.drain()
        if (appErrChunk.isNotEmpty() && appErrChunk.isNotBlank()) {
            println("app ERR: $appErrChunk")
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



package at.sljivic.aegis

import androidx.compose.runtime.*

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
        sb.setLength(0) // clear
        return out
    }
}

data class TracingResult(
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
       val sandbox_config = parsePolicyFile("/tmp/HackaTUM/socket_when.aegis");
   // val sandbox_config = parsePolicyFile("/tmp/HackaTUM/gen_policy.aegis")
    var syscalls_in_order = ArrayList<Syscall>()
    val traceResult = traceExecutable("/home/dominik/Workspace/HackaTUM/aegis/example_apps/a.out", listOf(), 60, sandbox_config)
    // we trust it will die at some point
    var last_time = false
    while (true) {
        val outChunk = traceResult.tracerOut.drain()
        if (outChunk.isNotEmpty() && outChunk.isNotBlank()) {
            val outs = outChunk.split("\n")
            for (out_syscall in outs) {
                if (out_syscall.isEmpty() || out_syscall.isBlank()) continue
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

        if (last_time) break

        if (!traceResult.tracerProc.isAlive()) {
            last_time = true
            traceResult.tracerOutThread.join()
            traceResult.tracerErrThread.join()
            traceResult.appErrThread.join()
            traceResult.appOutThread.join()
        }
        Thread.sleep(100)
    }

    return syscalls_in_order
}

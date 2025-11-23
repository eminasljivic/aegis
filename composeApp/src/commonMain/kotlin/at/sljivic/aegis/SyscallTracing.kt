package at.sljivic.aegis

import androidx.compose.runtime.*

enum class OperationType {
    File,
    Network,
    ProcessManagement,
    Memory,
    Signal,
    Time,
    Sync,
    Resources,
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
    fun append(line: String?) {
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

expect fun getTracerPath(): String

expect fun executeExecutable(
        executablePath: String,
        args: List<String> = emptyList(),
        timeoutSeconds: Long = 60
): TracingResult 

fun traceExecutable(
        executablePath: String,
        args: List<String>,
        timeoutSeconds: Long,
        sandbox: SandboxingOptions
): TracingResult {
    val executable = getTracerPath()

    val arguments = ArrayList<String>()
    println(sandbox.syscall_restrictions.size.toString())
    arguments.add(sandbox.syscall_restrictions.size.toString())
    for (sys in sandbox.syscall_restrictions) {
        arguments.add(sys.toString())
    }
    if (sandbox.syscall_restrictions_stage_2.isNotEmpty()) {
        arguments.add("-two-step")
        arguments.add(sandbox.condition.toString())
        arguments.add(sandbox.syscall_restrictions_stage_2.size.toString())
        for (sys in sandbox.syscall_restrictions_stage_2) {
            arguments.add(sys.toString())
        }
    } else {
        arguments.add("-one-step")
    }

    arguments.add(executablePath)
    arguments.addAll(args)

    println("Executing command: $executable ${arguments.joinToString(" ")}")

    val tracingRes = executeExecutable(executable, arguments)

    // println("\n--- Execution Result ---")
    // println("Exit Code: $code")
    // println("Output:")
    // println(output)
    // println("------------------------")

    return tracingRes
}

fun getSyscallList(path: String): ArrayList<Syscall> {
    val sandbox_config = parsePolicyFile("/tmp/HackaTUM/socket_when.aegis")
    // val sandbox_config = parsePolicyFile("/tmp/HackaTUM/gen_policy.aegis")
    var syscalls_in_order = ArrayList<Syscall>()
    val traceResult = traceExecutable(path, listOf(), 60, sandbox_config)
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
}

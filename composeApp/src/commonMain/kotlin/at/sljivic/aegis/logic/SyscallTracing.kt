package at.sljivic.aegis.logic

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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

sealed interface TraceEvent {
    data class SyscallEvent(val name: Syscall): TraceEvent
    data class TracerErr(val msg: String): TraceEvent
    data class AppOut(val msg: String): TraceEvent
    data class AppErr(val msg: String): TraceEvent
    data object Finished: TraceEvent
}

fun runTrace(path: String): Flow<TraceEvent> = flow {
    val sandboxConfig = SandboxingOptions(arrayListOf(), arrayListOf(), 1)
    val traceResult = traceExecutable(path, listOf(), 60, sandboxConfig)

    var times = 0
    var last = false

    while (true) {
        times += 1

        // -------- tracer OUT --------
        val outChunk = traceResult.tracerOut.drain()
        if (outChunk.isNotBlank()) {
            outChunk.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    emit(TraceEvent.SyscallEvent(syscallNumToSyscall(line.toInt())))
                }
            }
        }

        // -------- tracer ERR --------
        val errChunk = traceResult.tracerErr.drain()
        if (errChunk.isNotBlank()) {
            emit(TraceEvent.TracerErr(errChunk))
        }

        // -------- app OUT --------
        val appOutChunk = traceResult.appOut.drain()
        if (appOutChunk.isNotBlank()) {
            emit(TraceEvent.AppOut(appOutChunk))
        }

        // -------- app ERR --------
        val appErrChunk = traceResult.appErr.drain()
        if (appErrChunk.isNotBlank()) {
            emit(TraceEvent.AppErr(appErrChunk))
        }

        // --- termination ---
        if (last) {
            emit(TraceEvent.Finished)
            break
        }

        if (!traceResult.tracerProc.isAlive()) {
            last = true
            traceResult.tracerOutThread.join()
            traceResult.tracerErrThread.join()
            traceResult.appOutThread.join()
            traceResult.appErrThread.join()
        }

        delay(100)
        if (times > 20) {
            traceResult.tracerProc.destroyForcibly()
        }
    }
}

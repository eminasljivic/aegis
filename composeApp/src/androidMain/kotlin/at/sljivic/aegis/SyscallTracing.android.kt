package at.sljivic.aegis

actual fun traceExecutable(
    executablePath: String,
    args: List<String>,
    timeoutSeconds: Long,
    sandbox: SandboxingOptions
): TracingResult {
    val executable = "/data/data/at.sljivic.aegis/code_cache/tracer"

    val arguments = ArrayList<String>()
    println(sandbox.syscall_restrictions.size.toString())
    arguments.add(sandbox.syscall_restrictions.size.toString())
    for (sys in sandbox.syscall_restrictions) {
        arguments.add(sys.toString())
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

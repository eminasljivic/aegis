package at.sljivic.aegis

actual fun syscallNumToSyscall(num: Int): at.sljivic.aegis.Syscall {
    TODO("Not yet implemented")
}

actual fun syscallNameToNum(name: String): Int {
    TODO("Not yet implemented")
}

actual fun getNumSyscalls(): Int {
    TODO("Not yet implemented")
}

actual fun getSyscallsOfType(type: OperationType): ArrayList<String> {
    TODO("Not yet implemented")
}

actual fun traceExecutable(
    executablePath: String,
    args: List<String>,
    timeoutSeconds: Long,
    sandbox: SandboxingOptions
): String {
    TODO("Not yet implemented")
}
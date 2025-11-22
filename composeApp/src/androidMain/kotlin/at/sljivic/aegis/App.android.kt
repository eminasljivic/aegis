package at.sljivic.aegis

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher


actual fun syscallNumToSyscall(num: Int): Syscall {
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
): TracingResult {
    TODO("Not yet implemented")
}


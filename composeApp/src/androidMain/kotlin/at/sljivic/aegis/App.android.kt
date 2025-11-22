package at.sljivic.aegis

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult


private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

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
): String {
    TODO("Not yet implemented")
}


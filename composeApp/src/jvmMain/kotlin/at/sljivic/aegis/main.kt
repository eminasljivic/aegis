package at.sljivic.aegis

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Executes an external command (executable) using ProcessBuilder.
 * @param executablePath The path to the executable file.
 * @param args The arguments to pass to the executable.
 * @param timeoutSeconds The maximum time to wait for the process to finish.
 * @return A Pair containing the exit code and the captured output (or error).
 */
fun executeExecutable(
        executablePath: String,
        args: List<String> = emptyList(),
        timeoutSeconds: Long = 60
): Pair<Int, String> {
    // 1. Combine the executable path and its arguments into a single list of command parts
    val commandParts = mutableListOf(executablePath).apply { addAll(args) }

    // 2. Create and configure the ProcessBuilder
    val process =
            try {
                ProcessBuilder(commandParts)
                        .directory(
                                File(".")
                        ) // Optional: Set the working directory (defaults to current dir)
                        .redirectErrorStream(true) // Merges stdout and stderr into one stream
                        .start()
            } catch (e: Exception) {
                // Handle file not found, permission denied, etc.
                return Pair(-1, "Error starting process: ${e.message}")
            }

    // 3. Wait for the process to finish
    val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

    if (!finished) {
        // Process did not finish within the timeout
        process.destroyForcibly()
        return Pair(-2, "Process timed out after $timeoutSeconds seconds.")
    }

    // 4. Capture the output
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.exitValue()

    // 5. Return the result
    return Pair(exitCode, output.trim())
}

fun main() = application {
    Window(
            onCloseRequest = ::exitApplication,
            title = "aegis",
    ) {
        App()

        val executable = "/tmp/HackaTUM/tracer" // For Unix-like systems (Linux/macOS)
        // val executable = "cmd.exe" // Use "cmd.exe" for Windows
        val arguments = listOf("")

        println("Executing command: $executable ${arguments.joinToString(" ")}")

        val (code, output) = executeExecutable(executable, arguments)

        println("\n--- Execution Result ---")
        println("Exit Code: $code")
        println("Output:")
        println(output)
        println("------------------------")
    }
}

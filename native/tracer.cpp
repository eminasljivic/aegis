#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ptrace.h>
#include <sys/user.h>
#include <sys/wait.h>
#include <unistd.h>

// On x86-64, the syscall number is in the orig_rax register
#ifndef __x86_64__
#error "This code is written for x86-64 architecture"
#endif

// Function to handle the tracing logic
void run_tracer(pid_t child_pid) {
    int status;
    long syscall_num;

    // Wait for the child to stop after PTRACE_TRACEME and before execvp returns
    if (waitpid(child_pid, &status, 0) == -1) {
        perror("waitpid");
        return;
    }

    // Set PTRACE_O_TRACESYSGOOD option to distinguish syscall stops from other stops
    if (ptrace(PTRACE_SETOPTIONS, child_pid, 0, PTRACE_O_TRACESYSGOOD) == -1) {
        perror("ptrace SETOPTIONS");
        // Non-fatal, continue without the option
    }

    while (WIFSTOPPED(status)) {
        struct user_regs_struct regs;

        // 1. Continue the child and stop at the next syscall entry or exit
        if (ptrace(PTRACE_SYSCALL, child_pid, 0, 0) == -1) {
            // Check if the error is due to the process having exited
            if (errno == ESRCH)
                break;
            perror("ptrace SYSCALL (continue)");
            break;
        }

        // 2. Wait for the stop (either syscall entry or exit)
        if (waitpid(child_pid, &status, 0) == -1) {
            // Check if the error is due to the process having exited
            if (errno == ECHILD || errno == ESRCH)
                break;
            perror("waitpid");
            break;
        }

        // Check for child exit
        if (WIFEXITED(status)) {
            fprintf(stderr, "Child exited with status %d\n", WEXITSTATUS(status));
            break;
        }

        // Syscall stop is signaled by (WSTOPSIG(status) & 0x80)
        // or by PTRACE_O_TRACESYSGOOD resulting in SIGTRAP | 0x80
        if (WIFSTOPPED(status) && (WSTOPSIG(status) == (SIGTRAP | 0x80))) {

            // 3. Get the register values
            if (ptrace(PTRACE_GETREGS, child_pid, 0, &regs) == -1) {
                // Check if the error is due to the process having exited
                if (errno == ESRCH)
                    break;
                perror("ptrace GETREGS");
                break;
            }

            // On syscall entry (before it executes), the number is in orig_rax
            // On syscall exit (after it executes), the return value is in rax
            // Since we only want to report the number *once*, we check for a specific state.

            // For simplicity and only reporting the number, we report at *entry*
            // An even simpler approach is just to print at every stop and let the user see duplicates

            // For this simple version, we'll print at *every* stop (entry and exit)
            // and let the user see the duplicates which are expected with PTRACE_SYSCALL.

            syscall_num = regs.orig_rax;
            printf("%ld\n", syscall_num);
        } else {
            // If it stopped for another reason (e.g., signal delivery), continue it
            if (ptrace(PTRACE_CONT, child_pid, 0, WSTOPSIG(status)) == -1) {
                perror("ptrace CONT");
                break;
            }
        }
    }
}

// Main function
int main(int argc, char* argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <executable> [arg1 arg2 ...]\n", argv[0]);
        return EXIT_FAILURE;
    }

    pid_t pid = fork();

    if (pid == -1) {
        perror("fork");
        return EXIT_FAILURE;
    } else if (pid == 0) {
        // Child process: The tracee
        int fd = open("/dev/null", O_WRONLY);
        if (dup2(fd, 1) < 0) {
            // error handling
        }
        // Announce willingness to be traced
        if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
            perror("ptrace TRACEME");
            _exit(EXIT_FAILURE); // Use _exit in child after fork/ptrace
        }

        // Execute the target program and its arguments
        // argv[1] is the executable path, and argv+1 is the argument list (including the executable)
        execvp(argv[1], argv + 1);

        // execvp only returns if an error occurred
        perror("execvp");
        _exit(EXIT_FAILURE);
    } else {
        // Parent process: The tracer
        run_tracer(pid);
    }

    return EXIT_SUCCESS;
}
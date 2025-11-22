#include "sandboxer.h"
#include <cstdint>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <sys/ptrace.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/user.h>
#include <sys/wait.h>
#include <unistd.h>
#include <vector>

// On x86-64, the syscall number is in the orig_rax register
#ifndef __x86_64__
#error "This code is written for x86-64 architecture"
#endif

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

/**
 * Sets a file descriptor to non-blocking mode.
 * @param fd The file descriptor to modify.
 * @return 0 on success, or -1 on failure (and sets errno).
 */
int set_nonblocking(int fd) {
    // 1. Get the current file descriptor flags
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) {
        perror("fcntl F_GETFL");
        return -1;
    }

    // 2. Add the O_NONBLOCK flag to the existing flags
    flags |= O_NONBLOCK;

    // 3. Set the new flags
    if (fcntl(fd, F_SETFL, flags) == -1) {
        perror("fcntl F_SETFL");
        return -1;
    }

    return 0;
}

// Function to handle the tracing logic
void run_tracer(pid_t child_pid) {
    fprintf(stderr, "starting to trace...\n");
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
            int output_file = open("/tmp/HackaTUM/res", O_RDWR | O_CREAT);
            if (output_file < 0) {
                perror("open");
                fprintf(stderr, "Failed to open result file :(\n");
                exit(1);
            }
            std::string status_as_str = std::to_string(status);
            if (write(output_file, status_as_str.data(), status_as_str.size() + 1) < 0) {
                fprintf(stderr, "Failed to write to result file :(\n");
            }
            close(output_file);
            exit(0);
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
        fprintf(stderr, "Usage: %s <num_syscalls_to_restrict> [sysnr1 sysnr2 ... ] <executable> [arg1 arg2 ...]\n",
                argv[0]);
        return EXIT_FAILURE;
    }

    uint32_t num_syscalls_to_restrict = atoi(argv[1]);
    std::vector<uint32_t> syscalls_to_restrict;

    for (size_t i = 0; i < num_syscalls_to_restrict; ++i) {
        syscalls_to_restrict.push_back(atoi(argv[2 + i]));
    }

    pid_t pid = fork();

    if (pid == -1) {
        perror("fork");
        return EXIT_FAILURE;
    } else if (pid == 0) {
        //  fprintf(stderr, "uid: %ld\n", (uint32_t)getuid());
        // Child process: The tracee

    //    unlink("/tmp/HackaTUM/program_out_fifo");
        mkfifo("/tmp/HackaTUM/program_out_fifo", 0777); // open("/dev/null", O_WRONLY);
        // int rfd_dummy = open("/tmp/HackaTUM/program_out_fifo", O_RDONLY | O_NONBLOCK);
        int fd = open("/tmp/HackaTUM/program_out_fifo", O_WRONLY);
        if (fd < 0) {
            fprintf(stderr, "Error opening fd");
        }
        // Dummy reader to keep the FIFO alive

        // set_nonblocking(fd);
        if (dup2(fd, 1) < 0) {
            // error handling
            fprintf(stderr, "Error duplicating fd");
        }

        //unlink("/tmp/HackaTUM/program_err_fifo");
        mkfifo("/tmp/HackaTUM/program_err_fifo", 0777); // open("/dev/null", O_WRONLY);
        //  int rfd2_dummy = open("/tmp/HackaTUM/program_err_fifo", O_RDONLY | O_NONBLOCK);
        int fd2 = open("/tmp/HackaTUM/program_err_fifo", O_WRONLY);
        // set_nonblocking(fd2);
        if (dup2(fd2, 2) < 0) {
            // error handling
            fprintf(stderr, "Error duplicating fd2");
        }

        // Announce willingness to be traced
        if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
            perror("ptrace TRACEME");
            _exit(EXIT_FAILURE); // Use _exit in child after fork/ptrace
        }

        sandbox_current_process_seccomp(syscalls_to_restrict);
        execvp(argv[2 + num_syscalls_to_restrict], argv + 2 + num_syscalls_to_restrict);

        // execvp only returns if an error occurred
        perror("execvp");
        _exit(EXIT_FAILURE);
    } else {
        // Parent process: The tracer
        run_tracer(pid);
    }

    return EXIT_SUCCESS;
}
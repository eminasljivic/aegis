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

#if defined(__x86_64__)
#define SYSCALL_REG_FIELD orig_rax
#define ARCH_REGS_TYPE struct user_regs_struct
#elif defined(__aarch64__)
#define SYSCALL_REG_FIELD regs[8]
#define ARCH_REGS_TYPE struct user_regs_struct
#else
#error "Unsupported architecture"
#endif

#include <errno.h>
#include <fcntl.h>
#include <iostream>
#include <stdio.h>
#include <unistd.h>

#include <atomic>

std::atomic<bool> sandbox_is_two_step{false};
std::atomic<bool> update_sandbox{false};

#include <csignal>
#include <iostream>

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
#include <unordered_set>
std::unordered_set<uint32_t> syscalls_to_restrict_stage_2{};
uint32_t condition = UINT32_MAX;
bool in_stage_2 = false;

// Function to handle the tracing logic
void run_tracer(pid_t child_pid) {
    fprintf(stderr, "starting to trace...\n");
    int status;
    long syscall_num;

    // Wait for the child to stop after PTRACE_TRACEME and before execvp returns
    int waitpid_ret = waitpid(child_pid, &status, 0);
    if (waitpid_ret == -1) {
        // fprintf(stderr, "%d\n", errno);
        perror("waitpid");
        return;
    }

    // Set PTRACE_O_TRACESYSGOOD option to distinguish syscall stops from other stops
    if (ptrace(PTRACE_SETOPTIONS, child_pid, 0, PTRACE_O_TRACESYSGOOD) == -1) {
        perror("ptrace SETOPTIONS");
        // Non-fatal, continue without the option
    }

    while (WIFSTOPPED(status)) {
        ARCH_REGS_TYPE regs;

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

            syscall_num = regs.SYSCALL_REG_FIELD;
            printf("%ld\n", syscall_num);
            fprintf(stderr, "%ld\n", syscall_num);
            if (in_stage_2 && syscalls_to_restrict_stage_2.find(syscall_num) != syscalls_to_restrict_stage_2.end()) {
                // KILL ITTTTTTTTTTTTTTTTTTTTTTTTTTTTT
                if (kill(child_pid, SIGKILL) < 0) {
                    fprintf(stderr, "killing did not work\n");
                } else
                    fprintf(stderr, "Killed my child hehe :>\n");
            }

            if (condition != UINT32_MAX && syscall_num == condition) {
                fprintf(stderr, "entering stage 2\n");
                // sandbox_current_process_seccomp(syscalls_to_restrict_stage_2);
                in_stage_2 = true;
                condition = UINT32_MAX;
            }
        } else {
            // If it stopped for another reason (e.g., signal delivery), continue it
            if (ptrace(PTRACE_CONT, child_pid, 0, WSTOPSIG(status)) == -1) {
                perror("ptrace CONT");
                break;
            }
        }
    }
}
#include <cstring>

// Main function
int main(int argc, char* argv[]) {
    //   install_sigterm_handler();
    if (argc < 2) {
        fprintf(stderr,
                "Usage: %s <num_syscalls_to_restrict>"
                "[sysnr1 sysnr2 ... ]"
                "[-one-step | -two-step <num_syscalls_to_restrict>"
                "[sysnr1 sysnr2 ... ]] "
                "<executable> [arg1 arg2 ...]\n",
                argv[0]);
        return EXIT_FAILURE;
    }

    uint32_t num_syscalls_to_restrict = atoi(argv[1]);
    std::vector<uint32_t> syscalls_to_restrict;

    for (size_t i = 0; i < num_syscalls_to_restrict; ++i) {
        syscalls_to_restrict.push_back(atoi(argv[2 + i]));
    }

    bool stage_2 = (strncmp(argv[2 + num_syscalls_to_restrict], "-two-step", strlen("-two-step")) == 0);
    fprintf(stderr, "Two step sandbox? %d\n", stage_2);

    uint32_t num_syscalls_to_restrict_stage_2 = 0;

    if (stage_2) {
        fprintf(stderr, "two stage sandboxing active\n");
        condition = atoi(argv[2 + num_syscalls_to_restrict + 1]);
        fprintf(stderr, "condition: %ld\n", condition);
        num_syscalls_to_restrict_stage_2 = atoi(argv[2 + num_syscalls_to_restrict + 2]);

        fprintf(stderr, "supposedly %ld args for stage 2 sandbox\n", num_syscalls_to_restrict_stage_2);
        for (size_t i = 0; i < num_syscalls_to_restrict_stage_2; ++i) {
            syscalls_to_restrict_stage_2.insert(atoi(argv[2 + num_syscalls_to_restrict + 3 + i]));
        }
    }

    fprintf(stderr, "Stage 1: \n");
    for (auto& syscall : syscalls_to_restrict) {
        fprintf(stderr, "%d ", syscall);
    }
    fprintf(stderr, "\n Stage 2: \n");
    for (auto& syscall : syscalls_to_restrict_stage_2) {
        fprintf(stderr, "%d ", syscall);
    }
    fprintf(stderr, "\n\nexecuting %s\n",
            argv[2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1)]);

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

        // unlink("/tmp/HackaTUM/program_err_fifo");
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
        execvp(argv[2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1)],
               argv + 2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1));

        // execvp only returns if an error occurred
        perror("execvp");
        _exit(EXIT_FAILURE);
    } else {
        // Parent process: The tracer
        run_tracer(pid);
    }

    return EXIT_SUCCESS;
}
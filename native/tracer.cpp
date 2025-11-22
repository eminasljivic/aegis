#include "sandboxer.h"
#include <cstdint>
#include <cstring>
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
#include <csignal>
#include <iostream>
#include <unordered_set>
#include <atomic>

// yes globals are bad, no we do not care right now :c
std::unordered_set<uint32_t> syscalls_to_restrict_stage_2{};
uint32_t condition = UINT32_MAX;
bool in_stage_2 = false;

void run_tracer(pid_t child_pid) {
    fprintf(stderr, "starting to trace...\n");
    int status;
    long syscall_num;

    int waitpid_ret = waitpid(child_pid, &status, 0);
    if (waitpid_ret == -1) {
        perror("waitpid");
        return;
    }

    if (ptrace(PTRACE_SETOPTIONS, child_pid, 0, PTRACE_O_TRACESYSGOOD) == -1) {
        perror("ptrace SETOPTIONS");
    }

    while (WIFSTOPPED(status)) {
        ARCH_REGS_TYPE regs;

        if (ptrace(PTRACE_SYSCALL, child_pid, 0, 0) == -1) {
            if (errno == ESRCH)
                break;
            perror("ptrace SYSCALL (continue)");
            break;
        }

        if (waitpid(child_pid, &status, 0) == -1) {
            if (errno == ECHILD || errno == ESRCH)
                break;
            perror("waitpid");
            break;
        }

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

        if (WIFSTOPPED(status) && (WSTOPSIG(status) == (SIGTRAP | 0x80))) {

            if (ptrace(PTRACE_GETREGS, child_pid, 0, &regs) == -1) {
                // Check if the error is due to the process having exited
                if (errno == ESRCH)
                    break;
                perror("ptrace GETREGS");
                break;
            }

            syscall_num = regs.SYSCALL_REG_FIELD;
            printf("%ld\n", syscall_num);

            if (in_stage_2 && syscalls_to_restrict_stage_2.find(syscall_num) != syscalls_to_restrict_stage_2.end()) {
                // KILL ITTTTTTTTTTTTTTTTTTTTTTTTTTTTT
                if (kill(child_pid, SIGKILL) < 0) {
                    fprintf(stderr, "killing did not work\n");
                } else
                    fprintf(stderr, "Killed my child hehe :>\n");
            }

            if (condition != UINT32_MAX && syscall_num == condition) {
                fprintf(stderr, "entering stage 2\n");
                in_stage_2 = true;
                condition = UINT32_MAX;
            }
        } else {
            if (ptrace(PTRACE_CONT, child_pid, 0, WSTOPSIG(status)) == -1) {
                perror("ptrace CONT");
                break;
            }
        }
    }
}

int main(int argc, char* argv[]) {
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
        fprintf(stderr, "condition: %d\n", condition);
        num_syscalls_to_restrict_stage_2 = atoi(argv[2 + num_syscalls_to_restrict + 2]);

        fprintf(stderr, "supposedly %d args for stage 2 sandbox\n", num_syscalls_to_restrict_stage_2);
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
        mkfifo("/tmp/HackaTUM/program_out_fifo", 0777);
        int fd = open("/tmp/HackaTUM/program_out_fifo", O_WRONLY);
        if (fd < 0) {
            fprintf(stderr, "Error opening fd");
        }
        if (dup2(fd, 1) < 0) {
            fprintf(stderr, "Error duplicating fd");
        }

        mkfifo("/tmp/HackaTUM/program_err_fifo", 0777);
        int fd2 = open("/tmp/HackaTUM/program_err_fifo", O_WRONLY);
        if (dup2(fd2, 2) < 0) {
            fprintf(stderr, "Error duplicating fd2");
        }
        if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {
            perror("ptrace TRACEME");
            _exit(EXIT_FAILURE);
        }

        sandbox_current_process_seccomp(syscalls_to_restrict);
        execvp(argv[2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1)],
               argv + 2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1));

        perror("execvp");
        _exit(EXIT_FAILURE);
    } else {
        run_tracer(pid);
    }

    return EXIT_SUCCESS;
}
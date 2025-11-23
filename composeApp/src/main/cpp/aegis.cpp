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
#define SYSCALL_NUM(regs) ((regs).orig_rax)
#else
#define ARCH_REGS_TYPE struct user_pt_regs
#define SYSCALL_NUM(regs) ((regs).regs[8])
#define SYSCALL_RET(regs) ((regs).regs[0])
#endif

#ifndef NT_PRSTATUS
#define NT_PRSTATUS 1
#endif

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <jni.h>

#include <android/log.h>
#include <unordered_set>
#include <cstdint>

#define LOG_TAG "AegisNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// yes globals are bad, no we do not care right now :c
std::unordered_set<uint32_t> syscalls_to_restrict_stage_2{};
std::unordered_set<uint32_t> syscalls_to_restrict{};
uint32_t condition = UINT32_MAX;
bool in_stage_2 = false;
bool entering = true;

extern "C"
{
    void run_tracer(pid_t child_pid)
    {
        LOGI("starting to trace...\n");
        int status;
        long syscall_num;

        if (waitpid(child_pid, &status, 0) == -1)
        {
            LOGI("waitpid");
            return;
        }

        if (ptrace(PTRACE_SETOPTIONS, child_pid, 0, PTRACE_O_TRACESYSGOOD) == -1)
        {
            LOGI("ptrace SETOPTIONS");
            // Non-fatal, continue without the option
        }

        while (WIFSTOPPED(status))
        {
            ARCH_REGS_TYPE regs;

            if (ptrace(PTRACE_SYSCALL, child_pid, 0, 0) == -1)
            {
                if (errno == ESRCH)
                    break;
                LOGI("ptrace SYSCALL (continue)");
                break;
            }

            if (waitpid(child_pid, &status, 0) == -1)
            {
                if (errno == ECHILD || errno == ESRCH)
                    break;
                LOGI("waitpid");
                break;
            }

            if (WIFEXITED(status))
            {
                LOGI("Child exited with status %d\n", WEXITSTATUS(status));
                int output_file = open("/data/data/at.sljivic.aegis/files/res", O_RDWR | O_CREAT, 0644);
                if (output_file < 0)
                {
                    LOGI("open");
                    LOGI("Failed to open result file :(\n");
                    exit(1);
                }
                std::string status_as_str = std::to_string(status);
                if (write(output_file, status_as_str.data(), status_as_str.size() + 1) < 0)
                {
                    LOGI("Failed to write to result file :(\n");
                }
                close(output_file);
                exit(0);
            }
            if (WIFSTOPPED(status) && (WSTOPSIG(status) == (SIGTRAP | 0x80)))
            {
                struct iovec iov;
                iov.iov_base = &regs;
                iov.iov_len = sizeof(regs);

                if (ptrace(PTRACE_GETREGSET, child_pid, (void *)NT_PRSTATUS, &iov) == -1)
                {
                    if (errno == ESRCH)
                        break;
                    LOGI("ptrace GETREGS");
                    break;
                }

                if (entering)
                {
                    printf("%ld\n", SYSCALL_NUM(regs));
                    uint32_t syscall_num = SYSCALL_NUM(regs);

                    if (syscalls_to_restrict.find(syscall_num) != syscalls_to_restrict.end() || (in_stage_2 && syscalls_to_restrict_stage_2.find(syscall_num) != syscalls_to_restrict_stage_2.end()))
                    {
                        // KILL ITTTTTTTTTTTTTTTTTTTTTTTTTTTTT
                        if (kill(child_pid, SIGKILL) < 0)
                        {
                            // fprintf(stderr, "killing did not work\n");
                        }

                        //  fprintf(stderr, "Killed my child hehe :>\n");
                    }
                    if (condition != UINT32_MAX && syscall_num == condition)
                    {
                        // fprintf(stderr, "entering stage 2\n");
                        in_stage_2 = true;
                        condition = UINT32_MAX;
                    }
                }

                entering = !entering;
            }
            else
            {
                if (ptrace(PTRACE_CONT, child_pid, 0, WSTOPSIG(status)) == -1)
                {
                    LOGI("ptrace CONT");
                    break;
                }
            }
        }
    }

    JNIEXPORT jint JNICALL
    Java_at_sljivic_aegis_logic_Tracer_runNativeMain(JNIEnv *env, jobject obj, jobjectArray args)
    {
        pid_t pid = fork();

        if (pid == -1)
        {
            LOGI("fork");
            return EXIT_FAILURE;
        }
        else if (pid == 0)
        {
            mkfifo("/data/data/at.sljivic.aegis/files/tracer_out_fifo", 0777);
            int tracer_out_fd = open("/data/data/at.sljivic.aegis/files/tracer_out_fifo", O_WRONLY);

            if (tracer_out_fd < 0)
            {
                LOGI("Error opening tracer_fd");
            }
            if (dup2(tracer_out_fd, 1) < 0)
            {
                LOGI("Error duplicating tracer_fd");
            }

            mkfifo("/data/data/at.sljivic.aegis/files/tracer_err_fifo", 0777);
            int tracer_err_fd = open("/data/data/at.sljivic.aegis/files/tracer_err_fifo", O_WRONLY);

            if (tracer_err_fd < 0)
            {
                LOGI("Error opening tracer_fd");
            }
            if (dup2(tracer_err_fd, 2) < 0)
            {
                LOGI("Error duplicating tracer_fd");
            }

            // Get number of arguments
            jsize argc = env->GetArrayLength(args);

            // Allocate C-style argv array
            char **argv = (char **)malloc(sizeof(char *) * argc);

            for (jsize i = 0; i < argc; i++)
            {
                jstring str = (jstring)env->GetObjectArrayElement(args, i);
                const char *cStr = env->GetStringUTFChars(str, nullptr);
                argv[i] = strdup(cStr); // copy to argv
                env->ReleaseStringUTFChars(str, cStr);
            }

            if (argc < 2)
            {
                LOGI("Usage: %s <num_syscalls_to_restrict>"
                     "[sysnr1 sysnr2 ... ]"
                     "[-one-step | -two-step <num_syscalls_to_restrict>"
                     "[sysnr1 sysnr2 ... ]] "
                     "<executable> [arg1 arg2 ...]\n",
                     argv[0]);
                return EXIT_FAILURE;
            }

            uint32_t num_syscalls_to_restrict = atoi(argv[1]);

            for (size_t i = 0; i < num_syscalls_to_restrict; ++i)
            {
                syscalls_to_restrict.insert(atoi(argv[2 + i]));
            }

            bool stage_2 = (strncmp(argv[2 + num_syscalls_to_restrict], "-two-step", strlen("-two-step")) == 0);
            LOGI("Two step sandbox? %d\n", stage_2);

            uint32_t num_syscalls_to_restrict_stage_2 = 0;

            if (stage_2)
            {
                LOGI("two stage sandboxing active\n");
                condition = atoi(argv[2 + num_syscalls_to_restrict + 1]);
                LOGI("condition: %d\n", condition);
                num_syscalls_to_restrict_stage_2 = atoi(argv[2 + num_syscalls_to_restrict + 2]);

                LOGI("supposedly %d args for stage 2 sandbox\n", num_syscalls_to_restrict_stage_2);
                for (size_t i = 0; i < num_syscalls_to_restrict_stage_2; ++i)
                {
                    syscalls_to_restrict_stage_2.insert(atoi(argv[2 + num_syscalls_to_restrict + 3 + i]));
                }
            }

            LOGI("Stage 1: \n");
            for (auto &syscall : syscalls_to_restrict)
            {
                LOGI("%d ", syscall);
            }
            LOGI("\n Stage 2: \n");
            for (auto &syscall : syscalls_to_restrict_stage_2)
            {
                LOGI("%d ", syscall);
            }
            LOGI("\n\nexecuting %s\n",
                 argv[2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1)]);

            pid_t pid = fork();

            if (pid == -1)
            {
                LOGI("fork");
                return EXIT_FAILURE;
            }
            else if (pid == 0)
            {
                mkfifo("/data/data/at.sljivic.aegis/files/program_out_fifo", 0777);
                int fd = open("/data/data/at.sljivic.aegis/files/program_out_fifo", O_WRONLY);

                if (fd < 0)
                {
                    LOGI("Error opening fd");
                }

                if (dup2(fd, 1) < 0)
                {
                    LOGI("Error duplicating fd");
                }

                mkfifo("/data/data/at.sljivic.aegis/files/program_err_fifo", 0777);
                int fd2 = open("/data/data/at.sljivic.aegis/files/program_err_fifo", O_WRONLY);

                if (dup2(fd2, 2) < 0)
                {
                    LOGI("Error duplicating fd2");
                }

                // Announce willingness to be traced
                if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1)
                {
                    LOGI("ptrace TRACEME");
                    _exit(EXIT_FAILURE); // Use _exit in child after fork/ptrace
                }

                char **args = argv + 2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1);
                execvp(argv[2 + num_syscalls_to_restrict + num_syscalls_to_restrict_stage_2 + ((stage_2) ? 3 : 1)]), args);

                // execvp("ls", "."); // argv[2 + num_syscalls_to_restrict], argv + 2 + num_syscalls_to_restrict;

                LOGI("execvp");
                _exit(EXIT_FAILURE);
            }
            else
            {
                run_tracer(pid);
            }
        }
        return pid;
    }
}
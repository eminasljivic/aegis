package at.sljivic.aegis

import java.io.File
import kotlin.concurrent.thread
import kotlin.sequences.forEach

/**
 * Executes an external command (executable) using ProcessBuilder.
 * @param executablePath The path to the executable file.
 * @param args The arguments to pass to the executable.
 * @param timeoutSeconds The maximum time to wait for the process to finish.
 * @return A Pair containing the exit code and the captured output (or error).
 */
actual fun executeExecutable(
    executablePath: String,
    args: List<String>,
    timeoutSeconds: Long
): TracingResult {
    // TODO: Files have to be created before
    val fifoOut = File("/data/data/at.sljivic.aegis/files/program_out_fifo")
    val fifoErr = File("/data/data/at.sljivic.aegis/files/program_err_fifo")

    val tracerOut = File("/data/data/at.sljivic.aegis/files/tracer_out_fifo")
    val tracerErr = File("/data/data/at.sljivic.aegis/files/tracer_err_fifo")
    val outBuffer = StreamingBuffer()
    val errBuffer = StreamingBuffer()
    val procOutBuffer = StreamingBuffer()
    val procErrBuffer = StreamingBuffer()

    val tracerErrThread =
            thread(start = true) {
                tracerErr.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        procOutBuffer.append(line)
                        procOutBuffer.append("\n")
                    }
                }
            }

    val tracerOutThread =
            thread(start = true) {
                tracerOut.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        procOutBuffer.append(line)
                        procOutBuffer.append("\n")
                    }
                }
            }

    val outThread =
            thread(start = true) {
                fifoOut.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        outBuffer.append(line)
                        outBuffer.append("\n")
                    }
                }
            }

    val errThread =
            thread(start = true) {
                fifoErr.inputStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        errBuffer.append(line)
                        errBuffer.append("\n")
                    }
                }
            }

    val tracer = Tracer()
    val temp = tracer.runNativeMain(args.toTypedArray())

    val process =
            ProcessBuilder(
                            "sh",
                            "-c",
                            """
        while true; do
            if [ ! -d /proc/${temp} ]; then
                break
            fi
            sleep 1
        done
    """.trimIndent()
                    )
                    .start()

    // 5. Return the result
    return TracingResult(
            procOutBuffer,
            procErrBuffer,
            outBuffer,
            errBuffer,
            tracerOutThread,
            tracerErrThread,
            outThread,
            errThread,
            process
    )
}

var aarch64_android_syscall_num_to_name =
        arrayOf(
                Syscall("restart", OperationType.ProcessManagement),
                Syscall("exit", OperationType.ProcessManagement),
                Syscall("fork", OperationType.Unclassified),
                Syscall("read", OperationType.Unclassified),
                Syscall("write", OperationType.Unclassified),
                Syscall("open", OperationType.Unclassified),
                Syscall("close", OperationType.Unclassified),
                Syscall("creat", OperationType.Unclassified),
                Syscall("link", OperationType.Unclassified),
                Syscall("unlink", OperationType.Unclassified),
                Syscall("execve", OperationType.Unclassified),
                Syscall("chdir", OperationType.Unclassified),
                Syscall("time", OperationType.Unclassified),
                Syscall("mknod", OperationType.Unclassified),
                Syscall("chmod", OperationType.Unclassified),
                Syscall("lchown", OperationType.Unclassified),
                Syscall("lseek", OperationType.Unclassified),
                Syscall("getpid", OperationType.Unclassified),
                Syscall("mount", OperationType.Unclassified),
                Syscall("umount", OperationType.Unclassified),
                Syscall("setuid", OperationType.Unclassified),
                Syscall("getuid", OperationType.Unclassified),
                Syscall("stime", OperationType.Unclassified),
                Syscall("ptrace", OperationType.Unclassified),
                Syscall("alarm", OperationType.Unclassified),
                Syscall("pause", OperationType.Unclassified),
                Syscall("utime", OperationType.Unclassified),
                Syscall("sys_stty", OperationType.Unclassified),
                Syscall("sys_gtty", OperationType.Unclassified),
                Syscall("access3", OperationType.Unclassified),
                Syscall("nice4", OperationType.Unclassified),
                Syscall("sys_ftime", OperationType.Unclassified),
                Syscall("sync", OperationType.Unclassified),
                Syscall("kill", OperationType.Unclassified),
                Syscall("rename", OperationType.Unclassified),
                Syscall("mkdir", OperationType.Unclassified),
                Syscall("rmdir", OperationType.Unclassified),
                Syscall("dup", OperationType.Unclassified),
                Syscall("pipe", OperationType.Unclassified),
                Syscall("times", OperationType.Unclassified),
                Syscall("sys_prof", OperationType.Unclassified),
                Syscall("brk5", OperationType.Unclassified),
                Syscall("setgid6", OperationType.Unclassified),
                Syscall("getgid7", OperationType.Unclassified),
                Syscall("sys_signal", OperationType.Unclassified),
                Syscall("geteuid", OperationType.Unclassified),
                Syscall("getegid", OperationType.Unclassified),
                Syscall("acct", OperationType.Unclassified),
                Syscall("umount2", OperationType.Unclassified),
                Syscall("sys_lock", OperationType.Unclassified),
                Syscall("ioctl", OperationType.Unclassified),
                Syscall("fcntl", OperationType.Unclassified),
                Syscall("setpgid", OperationType.Unclassified),
                Syscall("sys_ulimit", OperationType.Unclassified),
                Syscall("sys_olduname", OperationType.Unclassified),
                Syscall("umask", OperationType.Unclassified),
                Syscall("chroot", OperationType.Unclassified),
                Syscall("ustat", OperationType.Unclassified),
                Syscall("dup2", OperationType.Unclassified),
                Syscall("getppid", OperationType.Unclassified),
                Syscall("getpgrp", OperationType.Unclassified),
                Syscall("setsid", OperationType.Unclassified),
                Syscall("sigaction", OperationType.Unclassified),
                Syscall("sys_sgetmask", OperationType.Unclassified),
                Syscall("sys_ssetmask", OperationType.Unclassified),
                Syscall("setreuid", OperationType.Unclassified),
                Syscall("setregid", OperationType.Unclassified),
                Syscall("sigsuspend", OperationType.Unclassified),
                Syscall("sigpending", OperationType.Unclassified),
                Syscall("sethostname", OperationType.Unclassified),
                Syscall("setrlimit", OperationType.Unclassified),
                Syscall("getrlimit", OperationType.Unclassified),
                Syscall("getrusage", OperationType.Unclassified),
                Syscall("gettimeofday", OperationType.Unclassified),
                Syscall("settimeofday", OperationType.Unclassified),
                Syscall("getgroups", OperationType.Unclassified),
                Syscall("setgroups", OperationType.Unclassified),
                Syscall("select", OperationType.Unclassified),
                Syscall("symlink", OperationType.Unclassified),
                Syscall("sys_lstat", OperationType.Unclassified),
                Syscall("readlink", OperationType.Unclassified),
                Syscall("uselib", OperationType.Unclassified),
                Syscall("swapon", OperationType.Unclassified),
                Syscall("reboot", OperationType.Unclassified),
                Syscall("readdir", OperationType.Unclassified),
                Syscall("mmap", OperationType.Unclassified),
                Syscall("munmap", OperationType.Unclassified),
                Syscall("truncate", OperationType.Unclassified),
                Syscall("ftruncate", OperationType.Unclassified),
                Syscall("fchmod", OperationType.Unclassified),
                Syscall("fchown", OperationType.Unclassified),
                Syscall("getpriority", OperationType.Unclassified),
                Syscall("setpriority", OperationType.Unclassified),
                Syscall("sys_profil", OperationType.Unclassified),
                Syscall("statfs", OperationType.Unclassified),
                Syscall("fstatfs", OperationType.Unclassified),
                Syscall("sys_ioperm", OperationType.Unclassified),
                Syscall("socketcall", OperationType.Unclassified),
                Syscall("syslog", OperationType.Unclassified),
                Syscall("setitimer", OperationType.Unclassified),
                Syscall("getitimer", OperationType.Unclassified),
                Syscall("stat", OperationType.Unclassified),
                Syscall("lstat", OperationType.Unclassified),
                Syscall("fstat", OperationType.Unclassified),
                Syscall("sys_uname", OperationType.Unclassified),
                Syscall("sys_iopl", OperationType.Unclassified),
                Syscall("vhangup", OperationType.Unclassified),
                Syscall("sys_idle", OperationType.Unclassified),
                Syscall("syscall", OperationType.Unclassified),
                Syscall("wait4", OperationType.Unclassified),
                Syscall("swapoff", OperationType.Unclassified),
                Syscall("sysinfo", OperationType.Unclassified),
                Syscall("ipc", OperationType.Unclassified),
                Syscall("fsync", OperationType.Unclassified),
                Syscall("sigreturn", OperationType.Unclassified),
                Syscall("clone", OperationType.Unclassified),
                Syscall("setdomainname", OperationType.Unclassified),
                Syscall("uname", OperationType.Unclassified),
                Syscall("sys_modify_ldt", OperationType.Unclassified),
                Syscall("adjtimex", OperationType.Unclassified),
                Syscall("mprotect", OperationType.Unclassified),
                Syscall("sigprocmask", OperationType.Unclassified),
                Syscall("sys_create_module", OperationType.Unclassified),
                Syscall("init_module", OperationType.Unclassified),
                Syscall("delete_module", OperationType.Unclassified),
                Syscall("sys_get_kernel_syms", OperationType.Unclassified),
                Syscall("quotactl", OperationType.Unclassified),
                Syscall("getpgid", OperationType.Unclassified),
                Syscall("fchdir", OperationType.Unclassified),
                Syscall("bdflush", OperationType.Unclassified),
                Syscall("sysfs", OperationType.Unclassified),
                Syscall("personality", OperationType.Unclassified),
                Syscall("sys_afs_syscall", OperationType.Unclassified),
                Syscall("setfsuid", OperationType.Unclassified),
                Syscall("setfsgid", OperationType.Unclassified),
                Syscall("llseek", OperationType.Unclassified),
                Syscall("getdents", OperationType.Unclassified),
                Syscall("newselect", OperationType.Unclassified),
                Syscall("flock", OperationType.Unclassified),
                Syscall("msync", OperationType.Unclassified),
                Syscall("readv", OperationType.Unclassified),
                Syscall("writev", OperationType.Unclassified),
                Syscall("getsid", OperationType.Unclassified),
                Syscall("fdatasync", OperationType.Unclassified),
                Syscall("sysctl", OperationType.Unclassified),
                Syscall("mlock", OperationType.Unclassified),
                Syscall("munlock", OperationType.Unclassified),
                Syscall("mlockall", OperationType.Unclassified),
                Syscall("munlockall", OperationType.Unclassified),
                Syscall("sched_setparam", OperationType.Unclassified),
                Syscall("sched_getparam", OperationType.Unclassified),
                Syscall("sched_setscheduler", OperationType.Unclassified),
                Syscall("sched_getscheduler", OperationType.Unclassified),
                Syscall("sched_yield", OperationType.Unclassified),
                Syscall("sched_get_priority_max", OperationType.Unclassified),
                Syscall("sched_get_priority_min", OperationType.Unclassified),
                Syscall("sched_rr_get_interval", OperationType.Unclassified),
                Syscall("nanosleep", OperationType.Unclassified),
                Syscall("mremap", OperationType.Unclassified),
                Syscall("setresuid", OperationType.Unclassified),
                Syscall("getresuid", OperationType.Unclassified),
                Syscall("sys_vm86", OperationType.Unclassified),
                Syscall("sys_query_module", OperationType.Unclassified),
                Syscall("poll", OperationType.Unclassified),
                Syscall("nfsservctl", OperationType.Unclassified),
                Syscall("setresgid", OperationType.Unclassified),
                Syscall("getresgid", OperationType.Unclassified),
                Syscall("prctl", OperationType.Unclassified),
                Syscall("rt_sigreturn", OperationType.Unclassified),
                Syscall("rt_sigaction", OperationType.Unclassified),
                Syscall("rt_sigprocmask", OperationType.Unclassified),
                Syscall("rt_sigpending", OperationType.Unclassified),
                Syscall("rt_sigtimedwait", OperationType.Unclassified),
                Syscall("rt_sigqueueinfo", OperationType.Unclassified),
                Syscall("rt_sigsuspend", OperationType.Unclassified),
                Syscall("pread64", OperationType.Unclassified),
                Syscall("pwrite64", OperationType.Unclassified),
                Syscall("chown", OperationType.Unclassified),
                Syscall("getcwd", OperationType.Unclassified),
                Syscall("capget", OperationType.Unclassified),
                Syscall("capset", OperationType.Unclassified),
                Syscall("sigaltstack", OperationType.Unclassified),
                Syscall("sendfile", OperationType.Unclassified),
                Syscall("reserved", OperationType.Unclassified),
                Syscall("reserved", OperationType.Unclassified),
                Syscall("vfork", OperationType.Unclassified),
                Syscall("ugetrlimit", OperationType.Unclassified),
                Syscall("mmap2", OperationType.Unclassified),
                Syscall("truncate64", OperationType.Unclassified),
                Syscall("ftruncate64", OperationType.Unclassified),
                Syscall("stat64", OperationType.Unclassified),
                Syscall("lstat64", OperationType.Unclassified),
                Syscall("fstat64", OperationType.Unclassified),
                Syscall("lchown32", OperationType.Unclassified),
                Syscall("getuid32", OperationType.Unclassified),
                Syscall("getgid32", OperationType.Unclassified),
                Syscall("geteuid32", OperationType.Unclassified),
                Syscall("getegid32", OperationType.Unclassified),
                Syscall("setreuid32", OperationType.Unclassified),
                Syscall("setregid32", OperationType.Unclassified),
                Syscall("getgroups32", OperationType.Unclassified),
                Syscall("setgroups32", OperationType.Unclassified),
                Syscall("fchown32", OperationType.Unclassified),
                Syscall("setresuid32", OperationType.Unclassified),
                Syscall("getresuid32", OperationType.Unclassified),
                Syscall("setresgid32", OperationType.Unclassified),
                Syscall("getresgid32", OperationType.Unclassified),
                Syscall("chown32", OperationType.Unclassified),
                Syscall("setuid32", OperationType.Unclassified),
                Syscall("setgid32", OperationType.Unclassified),
                Syscall("setfsuid32", OperationType.Unclassified),
                Syscall("setfsgid32", OperationType.Unclassified),
                Syscall("getdents64", OperationType.Unclassified),
                Syscall("pivot_root", OperationType.Unclassified),
                Syscall("mincore", OperationType.Unclassified),
                Syscall("madvise", OperationType.Unclassified),
                Syscall("fcntl64", OperationType.Unclassified),
                Syscall("tux", OperationType.Unclassified),
                Syscall("unused", OperationType.Unclassified),
                Syscall("gettid", OperationType.Unclassified),
                Syscall("readahead", OperationType.Unclassified),
                Syscall("setxattr", OperationType.Unclassified),
                Syscall("lsetxattr", OperationType.Unclassified),
                Syscall("fsetxattr", OperationType.Unclassified),
                Syscall("getxattr", OperationType.Unclassified),
                Syscall("lgetxattr", OperationType.Unclassified),
                Syscall("fgetxattr", OperationType.Unclassified),
                Syscall("listxattr", OperationType.Unclassified),
                Syscall("llistxattr", OperationType.Unclassified),
                Syscall("flistxattr", OperationType.Unclassified),
                Syscall("removexattr", OperationType.Unclassified),
                Syscall("lremovexattr", OperationType.Unclassified),
                Syscall("fremovexattr", OperationType.Unclassified),
                Syscall("tkill", OperationType.Unclassified),
                Syscall("sendfile64", OperationType.Unclassified),
                Syscall("futex", OperationType.Unclassified),
                Syscall("sched_setaffinity", OperationType.Unclassified),
                Syscall("sched_getaffinity", OperationType.Unclassified),
                Syscall("io_setup", OperationType.Unclassified),
                Syscall("io_destroy", OperationType.Unclassified),
                Syscall("io_getevents", OperationType.Unclassified),
                Syscall("io_submit", OperationType.Unclassified),
                Syscall("io_cancel", OperationType.Unclassified),
                Syscall("exit_group", OperationType.Unclassified),
                Syscall("lookup_dcookie", OperationType.Unclassified),
                Syscall("epoll_create", OperationType.Unclassified),
                Syscall("epoll_ctl", OperationType.Unclassified),
                Syscall("epoll_wait", OperationType.Unclassified),
                Syscall("remap_file_pages", OperationType.Unclassified),
                Syscall("set_thread_area", OperationType.Unclassified),
                Syscall("get_thread_area", OperationType.Unclassified),
                Syscall("set_tid_address", OperationType.Unclassified),
                Syscall("timer_create", OperationType.Unclassified),
                Syscall("timer_settime", OperationType.Unclassified),
                Syscall("timer_gettime", OperationType.Unclassified),
                Syscall("timer_getoverrun", OperationType.Unclassified),
                Syscall("timer_delete", OperationType.Unclassified),
                Syscall("clock_settime", OperationType.Unclassified),
                Syscall("clock_gettime", OperationType.Unclassified),
                Syscall("clock_getres", OperationType.Unclassified),
                Syscall("clock_nanosleep", OperationType.Unclassified),
                Syscall("statfs64", OperationType.Unclassified),
                Syscall("fstatfs64", OperationType.Unclassified),
                Syscall("tgkill", OperationType.Unclassified),
                Syscall("utimes", OperationType.Unclassified),
                Syscall("arm_fadvise64_64", OperationType.Unclassified),
                Syscall("pciconfig_iobase", OperationType.Unclassified),
                Syscall("pciconfig_read", OperationType.Unclassified),
                Syscall("pciconfig_write", OperationType.Unclassified),
                Syscall("mq_open", OperationType.Unclassified),
                Syscall("mq_unlink", OperationType.Unclassified),
                Syscall("mq_timedsend", OperationType.Unclassified),
                Syscall("mq_timedreceive", OperationType.Unclassified),
                Syscall("mq_notify", OperationType.Unclassified),
                Syscall("mq_getsetattr", OperationType.Unclassified),
                Syscall("waitid", OperationType.Unclassified),
                Syscall("socket", OperationType.Unclassified),
                Syscall("bind", OperationType.Unclassified),
                Syscall("connect", OperationType.Unclassified),
                Syscall("listen", OperationType.Unclassified),
                Syscall("accept", OperationType.Unclassified),
                Syscall("getsockname", OperationType.Unclassified),
                Syscall("getpeername", OperationType.Unclassified),
                Syscall("socketpair", OperationType.Unclassified),
                Syscall("send", OperationType.Unclassified),
                Syscall("sendto", OperationType.Unclassified),
                Syscall("recv", OperationType.Unclassified),
                Syscall("recvfrom", OperationType.Unclassified),
                Syscall("shutdown", OperationType.Unclassified),
                Syscall("setsockopt", OperationType.Unclassified),
                Syscall("getsockopt", OperationType.Unclassified),
                Syscall("sendmsg", OperationType.Unclassified),
                Syscall("recvmsg", OperationType.Unclassified),
                Syscall("semop", OperationType.Unclassified),
                Syscall("semget", OperationType.Unclassified),
                Syscall("semctl", OperationType.Unclassified),
                Syscall("msgsnd", OperationType.Unclassified),
                Syscall("msgrcv", OperationType.Unclassified),
                Syscall("msgget", OperationType.Unclassified),
                Syscall("msgctl", OperationType.Unclassified),
                Syscall("shmat", OperationType.Unclassified),
                Syscall("shmdt", OperationType.Unclassified),
                Syscall("shmget", OperationType.Unclassified),
                Syscall("shmctl", OperationType.Unclassified),
                Syscall("add_key", OperationType.Unclassified),
                Syscall("request_key", OperationType.Unclassified),
                Syscall("keyctl", OperationType.Unclassified),
                Syscall("semtimedop", OperationType.Unclassified),
                Syscall("vserver", OperationType.Unclassified),
                Syscall("ioprio_set", OperationType.Unclassified),
                Syscall("ioprio_get", OperationType.Unclassified),
                Syscall("inotify_init", OperationType.Unclassified),
                Syscall("inotify_add_watch", OperationType.Unclassified),
                Syscall("inotify_rm_watch", OperationType.Unclassified),
                Syscall("mbind", OperationType.Unclassified),
                Syscall("get_mempolicy", OperationType.Unclassified),
                Syscall("set_mempolicy", OperationType.Unclassified),
                Syscall("openat", OperationType.Unclassified),
                Syscall("mkdirat", OperationType.Unclassified),
                Syscall("mknodat", OperationType.Unclassified),
                Syscall("fchownat", OperationType.Unclassified),
                Syscall("futimesat", OperationType.Unclassified),
                Syscall("fstatat64", OperationType.Unclassified),
                Syscall("unlinkat", OperationType.Unclassified),
                Syscall("renameat", OperationType.Unclassified),
                Syscall("linkat", OperationType.Unclassified),
                Syscall("symlinkat", OperationType.Unclassified),
                Syscall("readlinkat", OperationType.Unclassified),
                Syscall("fchmodat", OperationType.Unclassified),
                Syscall("faccessat", OperationType.Unclassified),
                Syscall("pselect6", OperationType.Unclassified),
                Syscall("ppoll", OperationType.Unclassified),
                Syscall("unshare", OperationType.Unclassified),
                Syscall("set_robust_list", OperationType.Unclassified),
                Syscall("get_robust_list", OperationType.Unclassified),
                Syscall("splice", OperationType.Unclassified),
                Syscall("arm_sync_file_range", OperationType.Unclassified),
                Syscall("sync_file_range2", OperationType.Unclassified),
                Syscall("vmsplice", OperationType.Unclassified),
                Syscall("move_pages", OperationType.Unclassified),
                Syscall("getcpu", OperationType.Unclassified),
                Syscall("epoll_pwait", OperationType.Unclassified),
                Syscall("kexec_load", OperationType.Unclassified),
                Syscall("utimensat", OperationType.Unclassified),
                Syscall("signalfd", OperationType.Unclassified),
                Syscall("timerfd_create", OperationType.Unclassified),
                Syscall("eventfd", OperationType.Unclassified),
                Syscall("fallocate", OperationType.Unclassified),
                Syscall("timerfd_settime", OperationType.Unclassified),
                Syscall("timerfd_gettime", OperationType.Unclassified),
                Syscall("signalfd4", OperationType.Unclassified),
                Syscall("eventfd2", OperationType.Unclassified),
                Syscall("epoll_create1", OperationType.Unclassified),
                Syscall("dup3", OperationType.Unclassified),
                Syscall("pipe2", OperationType.Unclassified),
                Syscall("inotify_init1", OperationType.Unclassified),
                Syscall("preadv", OperationType.Unclassified),
                Syscall("pwritev", OperationType.Unclassified),
                Syscall("rt_tgsigqueueinfo", OperationType.Unclassified),
                Syscall("perf_event_open", OperationType.Unclassified),
                Syscall("recvmmsg", OperationType.Unclassified),
                Syscall("accept4", OperationType.Unclassified),
                Syscall("fanotify_init", OperationType.Unclassified),
                Syscall("fanotify_mark", OperationType.Unclassified),
                Syscall("prlimit64", OperationType.Unclassified),
                Syscall("name_to_handle_at", OperationType.Unclassified),
                Syscall("open_by_handle_at", OperationType.Unclassified),
                Syscall("clock_adjtime", OperationType.Unclassified),
                Syscall("syncfs", OperationType.Unclassified),
                Syscall("sendmmsg", OperationType.Unclassified),
                Syscall("setns", OperationType.Unclassified),
                Syscall("process_vm_readv", OperationType.Unclassified),
                Syscall("process_vm_writev", OperationType.Unclassified),
                Syscall("kcmp", OperationType.Unclassified),
                Syscall("finit_module", OperationType.Unclassified),
                Syscall("sched_setattr", OperationType.Unclassified),
                Syscall("sched_getattr", OperationType.Unclassified),
                Syscall("renameat2", OperationType.Unclassified),
                Syscall("seccomp", OperationType.Unclassified),
                Syscall("getrandom", OperationType.Unclassified),
                Syscall("memfd_create", OperationType.Unclassified),
                Syscall("bpf", OperationType.Unclassified)
        )

actual fun syscallNameToNum(name: String): Int {
    var i = 0
    while (i != aarch64_android_syscall_num_to_name.size) {
        if (aarch64_android_syscall_num_to_name.get(i).name == name) {
            return i
        }
        i += 1
    }
    return -1
}

actual fun syscallNumToSyscall(num: Int): Syscall {
    return aarch64_android_syscall_num_to_name.get(num)
}

actual fun getNumSyscalls(): Int {
    return aarch64_android_syscall_num_to_name.size
}

actual fun getSyscallsOfType(type: OperationType): ArrayList<String> {
    var fittingSyscalls = ArrayList<String>()
    for (syscall in aarch64_android_syscall_num_to_name) {
        if (syscall.type == type) {
            fittingSyscalls.add(syscall.name)
        }
    }
    return fittingSyscalls
}

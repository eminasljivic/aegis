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
                Syscall("restart", Operation.Unclassified),
                Syscall("exit", Operation.Unclassified),
                Syscall("fork", Operation.Unclassified),
                Syscall("read", Operation.Unclassified),
                Syscall("write", Operation.Unclassified),
                Syscall("open", Operation.Unclassified),
                Syscall("close", Operation.Unclassified),
                Syscall("creat", Operation.Unclassified),
                Syscall("link", Operation.Unclassified),
                Syscall("unlink", Operation.Unclassified),
                Syscall("execve", Operation.Unclassified),
                Syscall("chdir", Operation.Unclassified),
                Syscall("time", Operation.Unclassified),
                Syscall("mknod", Operation.Unclassified),
                Syscall("chmod", Operation.Unclassified),
                Syscall("lchown", Operation.Unclassified),
                Syscall("lseek", Operation.Unclassified),
                Syscall("getpid", Operation.Unclassified),
                Syscall("mount", Operation.Unclassified),
                Syscall("umount", Operation.Unclassified),
                Syscall("setuid", Operation.Unclassified),
                Syscall("getuid", Operation.Unclassified),
                Syscall("stime", Operation.Unclassified),
                Syscall("ptrace", Operation.Unclassified),
                Syscall("alarm", Operation.Unclassified),
                Syscall("pause", Operation.Unclassified),
                Syscall("utime", Operation.Unclassified),
                Syscall("sys_stty", Operation.Unclassified),
                Syscall("sys_gtty", Operation.Unclassified),
                Syscall("access3", Operation.Unclassified),
                Syscall("nice4", Operation.Unclassified),
                Syscall("sys_ftime", Operation.Unclassified),
                Syscall("sync", Operation.Unclassified),
                Syscall("kill", Operation.Unclassified),
                Syscall("rename", Operation.Unclassified),
                Syscall("mkdir", Operation.Unclassified),
                Syscall("rmdir", Operation.Unclassified),
                Syscall("dup", Operation.Unclassified),
                Syscall("pipe", Operation.Unclassified),
                Syscall("times", Operation.Unclassified),
                Syscall("sys_prof", Operation.Unclassified),
                Syscall("brk5", Operation.Unclassified),
                Syscall("setgid6", Operation.Unclassified),
                Syscall("getgid7", Operation.Unclassified),
                Syscall("sys_signal", Operation.Unclassified),
                Syscall("geteuid", Operation.Unclassified),
                Syscall("getegid", Operation.Unclassified),
                Syscall("acct", Operation.Unclassified),
                Syscall("umount2", Operation.Unclassified),
                Syscall("sys_lock", Operation.Unclassified),
                Syscall("ioctl", Operation.Unclassified),
                Syscall("fcntl", Operation.Unclassified),
                Syscall("setpgid", Operation.Unclassified),
                Syscall("sys_ulimit", Operation.Unclassified),
                Syscall("sys_olduname", Operation.Unclassified),
                Syscall("umask", Operation.Unclassified),
                Syscall("chroot", Operation.Unclassified),
                Syscall("ustat", Operation.Unclassified),
                Syscall("dup2", Operation.Unclassified),
                Syscall("getppid", Operation.Unclassified),
                Syscall("getpgrp", Operation.Unclassified),
                Syscall("setsid", Operation.Unclassified),
                Syscall("sigaction", Operation.Unclassified),
                Syscall("sys_sgetmask", Operation.Unclassified),
                Syscall("sys_ssetmask", Operation.Unclassified),
                Syscall("setreuid", Operation.Unclassified),
                Syscall("setregid", Operation.Unclassified),
                Syscall("sigsuspend", Operation.Unclassified),
                Syscall("sigpending", Operation.Unclassified),
                Syscall("sethostname", Operation.Unclassified),
                Syscall("setrlimit", Operation.Unclassified),
                Syscall("getrlimit", Operation.Unclassified),
                Syscall("getrusage", Operation.Unclassified),
                Syscall("gettimeofday", Operation.Unclassified),
                Syscall("settimeofday", Operation.Unclassified),
                Syscall("getgroups", Operation.Unclassified),
                Syscall("setgroups", Operation.Unclassified),
                Syscall("select", Operation.Unclassified),
                Syscall("symlink", Operation.Unclassified),
                Syscall("sys_lstat", Operation.Unclassified),
                Syscall("readlink", Operation.Unclassified),
                Syscall("uselib", Operation.Unclassified),
                Syscall("swapon", Operation.Unclassified),
                Syscall("reboot", Operation.Unclassified),
                Syscall("readdir", Operation.Unclassified),
                Syscall("mmap", Operation.Unclassified),
                Syscall("munmap", Operation.Unclassified),
                Syscall("truncate", Operation.Unclassified),
                Syscall("ftruncate", Operation.Unclassified),
                Syscall("fchmod", Operation.Unclassified),
                Syscall("fchown", Operation.Unclassified),
                Syscall("getpriority", Operation.Unclassified),
                Syscall("setpriority", Operation.Unclassified),
                Syscall("sys_profil", Operation.Unclassified),
                Syscall("statfs", Operation.Unclassified),
                Syscall("fstatfs", Operation.Unclassified),
                Syscall("sys_ioperm", Operation.Unclassified),
                Syscall("socketcall", Operation.Unclassified),
                Syscall("syslog", Operation.Unclassified),
                Syscall("setitimer", Operation.Unclassified),
                Syscall("getitimer", Operation.Unclassified),
                Syscall("stat", Operation.Unclassified),
                Syscall("lstat", Operation.Unclassified),
                Syscall("fstat", Operation.Unclassified),
                Syscall("sys_uname", Operation.Unclassified),
                Syscall("sys_iopl", Operation.Unclassified),
                Syscall("vhangup", Operation.Unclassified),
                Syscall("sys_idle", Operation.Unclassified),
                Syscall("syscall", Operation.Unclassified),
                Syscall("wait4", Operation.Unclassified),
                Syscall("swapoff", Operation.Unclassified),
                Syscall("sysinfo", Operation.Unclassified),
                Syscall("ipc", Operation.Unclassified),
                Syscall("fsync", Operation.Unclassified),
                Syscall("sigreturn", Operation.Unclassified),
                Syscall("clone", Operation.Unclassified),
                Syscall("setdomainname", Operation.Unclassified),
                Syscall("uname", Operation.Unclassified),
                Syscall("sys_modify_ldt", Operation.Unclassified),
                Syscall("adjtimex", Operation.Unclassified),
                Syscall("mprotect", Operation.Unclassified),
                Syscall("sigprocmask", Operation.Unclassified),
                Syscall("sys_create_module", Operation.Unclassified),
                Syscall("init_module", Operation.Unclassified),
                Syscall("delete_module", Operation.Unclassified),
                Syscall("sys_get_kernel_syms", Operation.Unclassified),
                Syscall("quotactl", Operation.Unclassified),
                Syscall("getpgid", Operation.Unclassified),
                Syscall("fchdir", Operation.Unclassified),
                Syscall("bdflush", Operation.Unclassified),
                Syscall("sysfs", Operation.Unclassified),
                Syscall("personality", Operation.Unclassified),
                Syscall("sys_afs_syscall", Operation.Unclassified),
                Syscall("setfsuid", Operation.Unclassified),
                Syscall("setfsgid", Operation.Unclassified),
                Syscall("llseek", Operation.Unclassified),
                Syscall("getdents", Operation.Unclassified),
                Syscall("newselect", Operation.Unclassified),
                Syscall("flock", Operation.Unclassified),
                Syscall("msync", Operation.Unclassified),
                Syscall("readv", Operation.Unclassified),
                Syscall("writev", Operation.Unclassified),
                Syscall("getsid", Operation.Unclassified),
                Syscall("fdatasync", Operation.Unclassified),
                Syscall("sysctl", Operation.Unclassified),
                Syscall("mlock", Operation.Unclassified),
                Syscall("munlock", Operation.Unclassified),
                Syscall("mlockall", Operation.Unclassified),
                Syscall("munlockall", Operation.Unclassified),
                Syscall("sched_setparam", Operation.Unclassified),
                Syscall("sched_getparam", Operation.Unclassified),
                Syscall("sched_setscheduler", Operation.Unclassified),
                Syscall("sched_getscheduler", Operation.Unclassified),
                Syscall("sched_yield", Operation.Unclassified),
                Syscall("sched_get_priority_max", Operation.Unclassified),
                Syscall("sched_get_priority_min", Operation.Unclassified),
                Syscall("sched_rr_get_interval", Operation.Unclassified),
                Syscall("nanosleep", Operation.Unclassified),
                Syscall("mremap", Operation.Unclassified),
                Syscall("setresuid", Operation.Unclassified),
                Syscall("getresuid", Operation.Unclassified),
                Syscall("sys_vm86", Operation.Unclassified),
                Syscall("sys_query_module", Operation.Unclassified),
                Syscall("poll", Operation.Unclassified),
                Syscall("nfsservctl", Operation.Unclassified),
                Syscall("setresgid", Operation.Unclassified),
                Syscall("getresgid", Operation.Unclassified),
                Syscall("prctl", Operation.Unclassified),
                Syscall("rt_sigreturn", Operation.Unclassified),
                Syscall("rt_sigaction", Operation.Unclassified),
                Syscall("rt_sigprocmask", Operation.Unclassified),
                Syscall("rt_sigpending", Operation.Unclassified),
                Syscall("rt_sigtimedwait", Operation.Unclassified),
                Syscall("rt_sigqueueinfo", Operation.Unclassified),
                Syscall("rt_sigsuspend", Operation.Unclassified),
                Syscall("pread64", Operation.Unclassified),
                Syscall("pwrite64", Operation.Unclassified),
                Syscall("chown", Operation.Unclassified),
                Syscall("getcwd", Operation.Unclassified),
                Syscall("capget", Operation.Unclassified),
                Syscall("capset", Operation.Unclassified),
                Syscall("sigaltstack", Operation.Unclassified),
                Syscall("sendfile", Operation.Unclassified),
                Syscall("reserved", Operation.Unclassified),
                Syscall("reserved", Operation.Unclassified),
                Syscall("vfork", Operation.Unclassified),
                Syscall("ugetrlimit", Operation.Unclassified),
                Syscall("mmap2", Operation.Unclassified),
                Syscall("truncate64", Operation.Unclassified),
                Syscall("ftruncate64", Operation.Unclassified),
                Syscall("stat64", Operation.Unclassified),
                Syscall("lstat64", Operation.Unclassified),
                Syscall("fstat64", Operation.Unclassified),
                Syscall("lchown32", Operation.Unclassified),
                Syscall("getuid32", Operation.Unclassified),
                Syscall("getgid32", Operation.Unclassified),
                Syscall("geteuid32", Operation.Unclassified),
                Syscall("getegid32", Operation.Unclassified),
                Syscall("setreuid32", Operation.Unclassified),
                Syscall("setregid32", Operation.Unclassified),
                Syscall("getgroups32", Operation.Unclassified),
                Syscall("setgroups32", Operation.Unclassified),
                Syscall("fchown32", Operation.Unclassified),
                Syscall("setresuid32", Operation.Unclassified),
                Syscall("getresuid32", Operation.Unclassified),
                Syscall("setresgid32", Operation.Unclassified),
                Syscall("getresgid32", Operation.Unclassified),
                Syscall("chown32", Operation.Unclassified),
                Syscall("setuid32", Operation.Unclassified),
                Syscall("setgid32", Operation.Unclassified),
                Syscall("setfsuid32", Operation.Unclassified),
                Syscall("setfsgid32", Operation.Unclassified),
                Syscall("getdents64", Operation.Unclassified),
                Syscall("pivot_root", Operation.Unclassified),
                Syscall("mincore", Operation.Unclassified),
                Syscall("madvise", Operation.Unclassified),
                Syscall("fcntl64", Operation.Unclassified),
                Syscall("tux", Operation.Unclassified),
                Syscall("unused", Operation.Unclassified),
                Syscall("gettid", Operation.Unclassified),
                Syscall("readahead", Operation.Unclassified),
                Syscall("setxattr", Operation.Unclassified),
                Syscall("lsetxattr", Operation.Unclassified),
                Syscall("fsetxattr", Operation.Unclassified),
                Syscall("getxattr", Operation.Unclassified),
                Syscall("lgetxattr", Operation.Unclassified),
                Syscall("fgetxattr", Operation.Unclassified),
                Syscall("listxattr", Operation.Unclassified),
                Syscall("llistxattr", Operation.Unclassified),
                Syscall("flistxattr", Operation.Unclassified),
                Syscall("removexattr", Operation.Unclassified),
                Syscall("lremovexattr", Operation.Unclassified),
                Syscall("fremovexattr", Operation.Unclassified),
                Syscall("tkill", Operation.Unclassified),
                Syscall("sendfile64", Operation.Unclassified),
                Syscall("futex", Operation.Unclassified),
                Syscall("sched_setaffinity", Operation.Unclassified),
                Syscall("sched_getaffinity", Operation.Unclassified),
                Syscall("io_setup", Operation.Unclassified),
                Syscall("io_destroy", Operation.Unclassified),
                Syscall("io_getevents", Operation.Unclassified),
                Syscall("io_submit", Operation.Unclassified),
                Syscall("io_cancel", Operation.Unclassified),
                Syscall("exit_group", Operation.Unclassified),
                Syscall("lookup_dcookie", Operation.Unclassified),
                Syscall("epoll_create", Operation.Unclassified),
                Syscall("epoll_ctl", Operation.Unclassified),
                Syscall("epoll_wait", Operation.Unclassified),
                Syscall("remap_file_pages", Operation.Unclassified),
                Syscall("set_thread_area", Operation.Unclassified),
                Syscall("get_thread_area", Operation.Unclassified),
                Syscall("set_tid_address", Operation.Unclassified),
                Syscall("timer_create", Operation.Unclassified),
                Syscall("timer_settime", Operation.Unclassified),
                Syscall("timer_gettime", Operation.Unclassified),
                Syscall("timer_getoverrun", Operation.Unclassified),
                Syscall("timer_delete", Operation.Unclassified),
                Syscall("clock_settime", Operation.Unclassified),
                Syscall("clock_gettime", Operation.Unclassified),
                Syscall("clock_getres", Operation.Unclassified),
                Syscall("clock_nanosleep", Operation.Unclassified),
                Syscall("statfs64", Operation.Unclassified),
                Syscall("fstatfs64", Operation.Unclassified),
                Syscall("tgkill", Operation.Unclassified),
                Syscall("utimes", Operation.Unclassified),
                Syscall("arm_fadvise64_64", Operation.Unclassified),
                Syscall("pciconfig_iobase", Operation.Unclassified),
                Syscall("pciconfig_read", Operation.Unclassified),
                Syscall("pciconfig_write", Operation.Unclassified),
                Syscall("mq_open", Operation.Unclassified),
                Syscall("mq_unlink", Operation.Unclassified),
                Syscall("mq_timedsend", Operation.Unclassified),
                Syscall("mq_timedreceive", Operation.Unclassified),
                Syscall("mq_notify", Operation.Unclassified),
                Syscall("mq_getsetattr", Operation.Unclassified),
                Syscall("waitid", Operation.Unclassified),
                Syscall("socket", Operation.Unclassified),
                Syscall("bind", Operation.Unclassified),
                Syscall("connect", Operation.Unclassified),
                Syscall("listen", Operation.Unclassified),
                Syscall("accept", Operation.Unclassified),
                Syscall("getsockname", Operation.Unclassified),
                Syscall("getpeername", Operation.Unclassified),
                Syscall("socketpair", Operation.Unclassified),
                Syscall("send", Operation.Unclassified),
                Syscall("sendto", Operation.Unclassified),
                Syscall("recv", Operation.Unclassified),
                Syscall("recvfrom", Operation.Unclassified),
                Syscall("shutdown", Operation.Unclassified),
                Syscall("setsockopt", Operation.Unclassified),
                Syscall("getsockopt", Operation.Unclassified),
                Syscall("sendmsg", Operation.Unclassified),
                Syscall("recvmsg", Operation.Unclassified),
                Syscall("semop", Operation.Unclassified),
                Syscall("semget", Operation.Unclassified),
                Syscall("semctl", Operation.Unclassified),
                Syscall("msgsnd", Operation.Unclassified),
                Syscall("msgrcv", Operation.Unclassified),
                Syscall("msgget", Operation.Unclassified),
                Syscall("msgctl", Operation.Unclassified),
                Syscall("shmat", Operation.Unclassified),
                Syscall("shmdt", Operation.Unclassified),
                Syscall("shmget", Operation.Unclassified),
                Syscall("shmctl", Operation.Unclassified),
                Syscall("add_key", Operation.Unclassified),
                Syscall("request_key", Operation.Unclassified),
                Syscall("keyctl", Operation.Unclassified),
                Syscall("semtimedop", Operation.Unclassified),
                Syscall("vserver", Operation.Unclassified),
                Syscall("ioprio_set", Operation.Unclassified),
                Syscall("ioprio_get", Operation.Unclassified),
                Syscall("inotify_init", Operation.Unclassified),
                Syscall("inotify_add_watch", Operation.Unclassified),
                Syscall("inotify_rm_watch", Operation.Unclassified),
                Syscall("mbind", Operation.Unclassified),
                Syscall("get_mempolicy", Operation.Unclassified),
                Syscall("set_mempolicy", Operation.Unclassified),
                Syscall("openat", Operation.Unclassified),
                Syscall("mkdirat", Operation.Unclassified),
                Syscall("mknodat", Operation.Unclassified),
                Syscall("fchownat", Operation.Unclassified),
                Syscall("futimesat", Operation.Unclassified),
                Syscall("fstatat64", Operation.Unclassified),
                Syscall("unlinkat", Operation.Unclassified),
                Syscall("renameat", Operation.Unclassified),
                Syscall("linkat", Operation.Unclassified),
                Syscall("symlinkat", Operation.Unclassified),
                Syscall("readlinkat", Operation.Unclassified),
                Syscall("fchmodat", Operation.Unclassified),
                Syscall("faccessat", Operation.Unclassified),
                Syscall("pselect6", Operation.Unclassified),
                Syscall("ppoll", Operation.Unclassified),
                Syscall("unshare", Operation.Unclassified),
                Syscall("set_robust_list", Operation.Unclassified),
                Syscall("get_robust_list", Operation.Unclassified),
                Syscall("splice", Operation.Unclassified),
                Syscall("arm_sync_file_range", Operation.Unclassified),
                Syscall("sync_file_range2", Operation.Unclassified),
                Syscall("vmsplice", Operation.Unclassified),
                Syscall("move_pages", Operation.Unclassified),
                Syscall("getcpu", Operation.Unclassified),
                Syscall("epoll_pwait", Operation.Unclassified),
                Syscall("kexec_load", Operation.Unclassified),
                Syscall("utimensat", Operation.Unclassified),
                Syscall("signalfd", Operation.Unclassified),
                Syscall("timerfd_create", Operation.Unclassified),
                Syscall("eventfd", Operation.Unclassified),
                Syscall("fallocate", Operation.Unclassified),
                Syscall("timerfd_settime", Operation.Unclassified),
                Syscall("timerfd_gettime", Operation.Unclassified),
                Syscall("signalfd4", Operation.Unclassified),
                Syscall("eventfd2", Operation.Unclassified),
                Syscall("epoll_create1", Operation.Unclassified),
                Syscall("dup3", Operation.Unclassified),
                Syscall("pipe2", Operation.Unclassified),
                Syscall("inotify_init1", Operation.Unclassified),
                Syscall("preadv", Operation.Unclassified),
                Syscall("pwritev", Operation.Unclassified),
                Syscall("rt_tgsigqueueinfo", Operation.Unclassified),
                Syscall("perf_event_open", Operation.Unclassified),
                Syscall("recvmmsg", Operation.Unclassified),
                Syscall("accept4", Operation.Unclassified),
                Syscall("fanotify_init", Operation.Unclassified),
                Syscall("fanotify_mark", Operation.Unclassified),
                Syscall("prlimit64", Operation.Unclassified),
                Syscall("name_to_handle_at", Operation.Unclassified),
                Syscall("open_by_handle_at", Operation.Unclassified),
                Syscall("clock_adjtime", Operation.Unclassified),
                Syscall("syncfs", Operation.Unclassified),
                Syscall("sendmmsg", Operation.Unclassified),
                Syscall("setns", Operation.Unclassified),
                Syscall("process_vm_readv", Operation.Unclassified),
                Syscall("process_vm_writev", Operation.Unclassified),
                Syscall("kcmp", Operation.Unclassified),
                Syscall("finit_module", Operation.Unclassified),
                Syscall("sched_setattr", Operation.Unclassified),
                Syscall("sched_getattr", Operation.Unclassified),
                Syscall("renameat2", Operation.Unclassified),
                Syscall("seccomp", Operation.Unclassified),
                Syscall("getrandom", Operation.Unclassified),
                Syscall("memfd_create", Operation.Unclassified),
                Syscall("bpf", Operation.Unclassified)
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

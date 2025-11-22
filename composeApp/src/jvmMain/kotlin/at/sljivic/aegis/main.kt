package at.sljivic.aegis

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import kotlin.concurrent.thread

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
): TracingResult {
        // 1. Combine the executable path and its arguments into a single list of command parts
        val commandParts = mutableListOf(executablePath).apply { addAll(args) }

        val fifoOut = File("/tmp/HackaTUM/program_out_fifo")
        val fifoErr = File("/tmp/HackaTUM/program_err_fifo")
        val outBuffer = StreamingBuffer()
        val errBuffer = StreamingBuffer()
        val procOutBuffer = StreamingBuffer()
        val procErrBuffer = StreamingBuffer()

        // Start readers in background threads
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

        // 2. Create and configure the ProcessBuilder
        val process =
                ProcessBuilder(commandParts)
                        .directory(
                                File(".")
                        ) // Optional: Set the working directory (defaults to current dir)
                        .redirectErrorStream(false) // Merges stdout and stderr into one stream
                        .start()

        // Continuous process stdout reader
        val procOutThread =
                thread(start = true) {
                        process.inputStream.bufferedReader().useLines { lines ->
                                lines.forEach {
                                        procOutBuffer.append(it)
                                        procOutBuffer.append("\n".toString())
                                }
                        }
                }

        // Continuous process stderr reader
        val procErrThread =
                thread(start = true) {
                        process.errorStream.bufferedReader().useLines { lines ->
                                lines.forEach {
                                        procErrBuffer.append(it)
                                        procErrBuffer.append("\n".toString())
                                }
                        }
                }

        // 4. Capture the output
        // val output = process.inputStream.bufferedReader().use { it.readText() }
        // val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
        // println(errorOutput)

        // val exitCode = process.exitValue()

        // 5. Return the result
        return TracingResult(
                procOutBuffer,
                procErrBuffer,
                outBuffer,
                errBuffer,
                procOutThread,
                procErrThread,
                outThread,
                errThread,
                process
        )
}

val x64_linux_syscall_num_to_name =
        arrayOf(
                Syscall("read", OperationType.File),
                Syscall("write", OperationType.File),
                Syscall("open", OperationType.File),
                Syscall("close", OperationType.File),
                Syscall("stat", OperationType.File),
                Syscall("fstat", OperationType.File),
                Syscall("lstat", OperationType.File),
                Syscall("poll", OperationType.File),
                Syscall("lseek", OperationType.File),
                Syscall("mmap", OperationType.Memory),
                Syscall("mprotect", OperationType.Memory),
                Syscall("munmap", OperationType.Memory),
                Syscall("brk", OperationType.Memory),
                Syscall("rt_sigaction", OperationType.Signal),
                Syscall("rt_sigprocmask", OperationType.Signal),
                Syscall("rt_sigreturn", OperationType.Signal),
                Syscall("ioctl", OperationType.File),
                Syscall("pread64", OperationType.File),
                Syscall("pwrite64", OperationType.File),
                Syscall("readv", OperationType.File),
                Syscall("writev", OperationType.File),
                Syscall("access", OperationType.File),
                Syscall("pipe", OperationType.File),
                Syscall("select", OperationType.File),
                Syscall("sched_yield", OperationType.Unclassified),
                Syscall("mremap", OperationType.Memory),
                Syscall("msync", OperationType.Memory),
                Syscall("mincore", OperationType.Unclassified),
                Syscall("madvise", OperationType.Memory),
                Syscall("shmget", OperationType.Memory),
                Syscall("shmat", OperationType.Memory),
                Syscall("shmctl", OperationType.Memory),
                Syscall("dup", OperationType.File),
                Syscall("dup2", OperationType.File),
                Syscall("pause", OperationType.Time),
                Syscall("nanosleep", OperationType.Time),
                Syscall("getitimer", OperationType.Time),
                Syscall("alarm", OperationType.Time),
                Syscall("setitimer", OperationType.Time),
                Syscall("getpid", OperationType.ProcessManagement),
                Syscall("sendfile", OperationType.Network),
                Syscall("socket", OperationType.Network),
                Syscall("connect", OperationType.Network),
                Syscall("accept", OperationType.Network),
                Syscall("sendto", OperationType.Network),
                Syscall("recvfrom", OperationType.Network),
                Syscall("sendmsg", OperationType.Network),
                Syscall("recvmsg", OperationType.Network),
                Syscall("shutdown", OperationType.ProcessManagement),
                Syscall("bind", OperationType.Network),
                Syscall("listen", OperationType.Network),
                Syscall("getsockname", OperationType.Network),
                Syscall("getpeername", OperationType.Network),
                Syscall("socketpair", OperationType.Network),
                Syscall("setsockopt", OperationType.Network),
                Syscall("getsockopt", OperationType.Network),
                Syscall("clone", OperationType.ProcessManagement),
                Syscall("fork", OperationType.ProcessManagement),
                Syscall("vfork", OperationType.ProcessManagement),
                Syscall("execve", OperationType.ProcessManagement),
                Syscall("exit", OperationType.ProcessManagement),
                Syscall("wait4", OperationType.ProcessManagement),
                Syscall("kill", OperationType.ProcessManagement),
                Syscall("uname", OperationType.Unclassified),
                Syscall("semget", OperationType.Sync),
                Syscall("semop", OperationType.Sync),
                Syscall("semctl", OperationType.Sync),
                Syscall("shmdt", OperationType.Memory),
                Syscall("msgget", OperationType.Network),
                Syscall("msgsnd", OperationType.Network),
                Syscall("msgrcv", OperationType.Network),
                Syscall("msgctl", OperationType.Network),
                Syscall("fcntl", OperationType.File),
                Syscall("flock", OperationType.File),
                Syscall("fsync", OperationType.File),
                Syscall("fdatasync", OperationType.File),
                Syscall("truncate", OperationType.File),
                Syscall("ftruncate", OperationType.File),
                Syscall("getdents", OperationType.File),
                Syscall("getcwd", OperationType.File),
                Syscall("chdir", OperationType.File),
                Syscall("fchdir", OperationType.File),
                Syscall("rename", OperationType.File),
                Syscall("mkdir", OperationType.File),
                Syscall("rmdir", OperationType.File),
                Syscall("creat", OperationType.File),
                Syscall("link", OperationType.File),
                Syscall("unlink", OperationType.File),
                Syscall("symlink", OperationType.File),
                Syscall("readlink", OperationType.File),
                Syscall("chmod", OperationType.File),
                Syscall("fchmod", OperationType.File),
                Syscall("chown", OperationType.File),
                Syscall("fchown", OperationType.File),
                Syscall("lchown", OperationType.File),
                Syscall("umask", OperationType.File),
                Syscall("gettimeofday", OperationType.Time),
                Syscall("getrlimit", OperationType.Resources),
                Syscall("getrusage", OperationType.Resources),
                Syscall("sysinfo", OperationType.Unclassified),
                Syscall("times", OperationType.Time),
                Syscall("ptrace", OperationType.Unclassified),
                Syscall("getuid", OperationType.ProcessManagement),
                Syscall("syslog", OperationType.Unclassified),
                Syscall("getgid", OperationType.ProcessManagement),
                Syscall("setuid", OperationType.ProcessManagement),
                Syscall("setgid", OperationType.ProcessManagement),
                Syscall("geteuid", OperationType.ProcessManagement),
                Syscall("getegid", OperationType.ProcessManagement),
                Syscall("setpgid", OperationType.ProcessManagement),
                Syscall("getppid", OperationType.ProcessManagement),
                Syscall("getpgrp", OperationType.ProcessManagement),
                Syscall("setsid", OperationType.ProcessManagement),
                Syscall("setreuid", OperationType.ProcessManagement),
                Syscall("setregid", OperationType.ProcessManagement),
                Syscall("getgroups", OperationType.ProcessManagement),
                Syscall("setgroups", OperationType.ProcessManagement),
                Syscall("setresuid", OperationType.ProcessManagement),
                Syscall("getresuid", OperationType.ProcessManagement),
                Syscall("setresgid", OperationType.ProcessManagement),
                Syscall("getresgid", OperationType.ProcessManagement),
                Syscall("getpgid", OperationType.ProcessManagement),
                Syscall("setfsuid", OperationType.ProcessManagement),
                Syscall("setfsgid", OperationType.ProcessManagement),
                Syscall("getsid", OperationType.ProcessManagement),
                Syscall("capget", OperationType.ProcessManagement),
                Syscall("capset", OperationType.ProcessManagement),
                Syscall("rt_sigpending", OperationType.Signal),
                Syscall("rt_sigtimedwait", OperationType.Signal),
                Syscall("rt_sigqueueinfo", OperationType.Signal),
                Syscall("rt_sigsuspend", OperationType.Signal),
                Syscall("sigaltstack", OperationType.Signal),
                Syscall("utime", OperationType.Time),
                Syscall("mknod", OperationType.File),
                Syscall("uselib", OperationType.File),
                Syscall("personality", OperationType.ProcessManagement),
                Syscall("ustat", OperationType.File),
                Syscall("statfs", OperationType.File),
                Syscall("fstatfs", OperationType.File),
                Syscall("sysfs", OperationType.File),
                Syscall("getpriority", OperationType.ProcessManagement),
                Syscall("setpriority", OperationType.ProcessManagement),
                Syscall("sched_setparam", OperationType.ProcessManagement),
                Syscall("sched_getparam", OperationType.ProcessManagement),
                Syscall("sched_setscheduler", OperationType.ProcessManagement),
                Syscall("sched_getscheduler", OperationType.ProcessManagement),
                Syscall("sched_get_priority_max", OperationType.ProcessManagement),
                Syscall("sched_get_priority_min", OperationType.ProcessManagement),
                Syscall("sched_rr_get_interval", OperationType.ProcessManagement),
                Syscall("mlock", OperationType.Sync),
                Syscall("munlock", OperationType.Sync),
                Syscall("mlockall", OperationType.Sync),
                Syscall("munlockall", OperationType.Sync),
                Syscall("vhangup", OperationType.ProcessManagement),
                Syscall("modify_ldt", OperationType.Unclassified),
                Syscall("pivot_root", OperationType.Unclassified),
                Syscall("_sysctl", OperationType.Unclassified),
                Syscall("prctl", OperationType.Resources),
                Syscall("arch_prctl", OperationType.Resources),
                Syscall("adjtimex", OperationType.Unclassified),
                Syscall("setrlimit", OperationType.Resources),
                Syscall("chroot", OperationType.ProcessManagement),
                Syscall("sync", OperationType.Sync),
                Syscall("acct", OperationType.Unclassified),
                Syscall("settimeofday", OperationType.Time),
                Syscall("mount", OperationType.File),
                Syscall("umount2", OperationType.File),
                Syscall("swapon", OperationType.File),
                Syscall("swapoff", OperationType.File),
                Syscall("reboot", OperationType.ProcessManagement),
                Syscall("sethostname", OperationType.Resources),
                Syscall("setdomainname", OperationType.Resources),
                Syscall("iopl", OperationType.File),
                Syscall("ioperm", OperationType.File),
                Syscall("create_module", OperationType.ProcessManagement),
                Syscall("init_module", OperationType.ProcessManagement),
                Syscall("delete_module", OperationType.ProcessManagement),
                Syscall("get_kernel_syms", OperationType.ProcessManagement),
                Syscall("query_module", OperationType.ProcessManagement),
                Syscall("quotactl", OperationType.Resources),
                Syscall("nfsservctl", OperationType.Unclassified),
                Syscall("getpmsg", OperationType.Unclassified),
                Syscall("putpmsg", OperationType.Unclassified),
                Syscall("afs_syscall", OperationType.Unclassified),
                Syscall("tuxcall", OperationType.Unclassified),
                Syscall("security", OperationType.Unclassified),
                Syscall("gettid", OperationType.Time),
                Syscall("readahead", OperationType.File),
                Syscall("setxattr", OperationType.File),
                Syscall("lsetxattr", OperationType.File),
                Syscall("fsetxattr", OperationType.File),
                Syscall("getxattr", OperationType.File),
                Syscall("lgetxattr", OperationType.File),
                Syscall("fgetxattr", OperationType.File),
                Syscall("listxattr", OperationType.File),
                Syscall("llistxattr", OperationType.File),
                Syscall("flistxattr", OperationType.File),
                Syscall("removexattr", OperationType.File),
                Syscall("lremovexattr", OperationType.File),
                Syscall("fremovexattr", OperationType.File),
                Syscall("tkill", OperationType.Unclassified),
                Syscall("time", OperationType.ProcessManagement),
                Syscall("futex", OperationType.Sync),
                Syscall("sched_setaffinity", OperationType.ProcessManagement),
                Syscall("sched_getaffinity", OperationType.ProcessManagement),
                Syscall("set_thread_area", OperationType.ProcessManagement),
                Syscall("io_setup", OperationType.File),
                Syscall("io_destroy", OperationType.File),
                Syscall("io_getevents", OperationType.File),
                Syscall("io_submit", OperationType.File),
                Syscall("io_cancel", OperationType.File),
                Syscall("get_thread_area", OperationType.ProcessManagement),
                Syscall("lookup_dcookie", OperationType.Unclassified),
                Syscall("epoll_create", OperationType.Unclassified),
                Syscall("epoll_ctl_old", OperationType.Unclassified),
                Syscall("epoll_wait_old", OperationType.Unclassified),
                Syscall("remap_file_pages", OperationType.Unclassified),
                Syscall("getdents64", OperationType.File),
                Syscall("set_tid_address", OperationType.ProcessManagement),
                Syscall("restart_syscall", OperationType.Unclassified),
                Syscall("semtimedop", OperationType.Unclassified),
                Syscall("fadvise64", OperationType.Unclassified),
                Syscall("timer_create", OperationType.Time),
                Syscall("timer_settime", OperationType.Time),
                Syscall("timer_gettime", OperationType.Time),
                Syscall("timer_getoverrun", OperationType.Time),
                Syscall("timer_delete", OperationType.Time),
                Syscall("clock_settime", OperationType.Time),
                Syscall("clock_gettime", OperationType.Time),
                Syscall("clock_getres", OperationType.Time),
                Syscall("clock_nanosleep", OperationType.Time),
                Syscall("exit_group", OperationType.ProcessManagement),
                Syscall("epoll_wait", OperationType.ProcessManagement),
                Syscall("epoll_ctl", OperationType.ProcessManagement),
                Syscall("tgkill", OperationType.ProcessManagement),
                Syscall("utimes", OperationType.Unclassified),
                Syscall("vserver", OperationType.Unclassified),
                Syscall("mbind", OperationType.Unclassified),
                Syscall("set_mempolicy", OperationType.ProcessManagement),
                Syscall("get_mempolicy", OperationType.ProcessManagement),
                Syscall("mq_open", OperationType.ProcessManagement),
                Syscall("mq_unlink", OperationType.ProcessManagement),
                Syscall("mq_timedsend", OperationType.ProcessManagement),
                Syscall("mq_timedreceive", OperationType.ProcessManagement),
                Syscall("mq_notify", OperationType.ProcessManagement),
                Syscall("mq_getsetattr", OperationType.ProcessManagement),
                Syscall("kexec_load", OperationType.ProcessManagement),
                Syscall("waitid", OperationType.ProcessManagement),
                Syscall("add_key", OperationType.Unclassified),
                Syscall("request_key", OperationType.Unclassified),
                Syscall("keyctl", OperationType.Unclassified),
                Syscall("ioprio_set", OperationType.Unclassified),
                Syscall("ioprio_get", OperationType.Unclassified),
                Syscall("inotify_init", OperationType.Unclassified),
                Syscall("inotify_add_watch", OperationType.Unclassified),
                Syscall("inotify_rm_watch", OperationType.Unclassified),
                Syscall("migrate_pages", OperationType.Unclassified),
                Syscall("openat", OperationType.File),
                Syscall("mkdirat", OperationType.File),
                Syscall("mknodat", OperationType.File),
                Syscall("fchownat", OperationType.File),
                Syscall("futimesat", OperationType.Unclassified),
                Syscall("newfstatat", OperationType.Unclassified),
                Syscall("unlinkat", OperationType.File),
                Syscall("renameat", OperationType.Unclassified),
                Syscall("linkat", OperationType.File),
                Syscall("symlinkat", OperationType.File),
                Syscall("readlinkat", OperationType.File),
                Syscall("fchmodat", OperationType.Unclassified),
                Syscall("faccessat", OperationType.Unclassified),
                Syscall("pselect6", OperationType.Unclassified),
                Syscall("ppoll", OperationType.Unclassified),
                Syscall("unshare", OperationType.Memory),
                Syscall("set_robust_list", OperationType.ProcessManagement),
                Syscall("get_robust_list", OperationType.ProcessManagement),
                Syscall("splice", OperationType.Unclassified),
                Syscall("tee", OperationType.File),
                Syscall("sync_file_range", OperationType.Unclassified),
                Syscall("vmsplice", OperationType.Unclassified),
                Syscall("move_pages", OperationType.Unclassified),
                Syscall("utimensat", OperationType.Unclassified),
                Syscall("epoll_pwait", OperationType.Unclassified),
                Syscall("signalfd", OperationType.Unclassified),
                Syscall("timerfd_create", OperationType.Unclassified),
                Syscall("eventfd", OperationType.Unclassified),
                Syscall("fallocate", OperationType.Unclassified),
                Syscall("timerfd_settime", OperationType.Unclassified),
                Syscall("timerfd_gettime", OperationType.Unclassified),
                Syscall("accept4", OperationType.Network),
                Syscall("signalfd4", OperationType.Unclassified),
                Syscall("eventfd2", OperationType.Unclassified),
                Syscall("epoll_create1", OperationType.Unclassified),
                Syscall("dup3", OperationType.File),
                Syscall("pipe2", OperationType.File),
                Syscall("inotify_init1", OperationType.Unclassified),
                Syscall("preadv", OperationType.File),
                Syscall("pwritev", OperationType.File),
                Syscall("rt_tgsigqueueinfo", OperationType.Unclassified),
                Syscall("perf_event_open", OperationType.Unclassified),
                Syscall("recvmmsg", OperationType.Network),
                Syscall("fanotify_init", OperationType.Unclassified),
                Syscall("fanotify_mark", OperationType.Unclassified),
                Syscall("prlimit64", OperationType.ProcessManagement),
                Syscall("name_to_handle_at", OperationType.Unclassified),
                Syscall("open_by_handle_at", OperationType.Unclassified),
                Syscall("clock_adjtime", OperationType.Unclassified),
                Syscall("syncfs", OperationType.File),
                Syscall("sendmmsg", OperationType.Network),
                Syscall("setns", OperationType.Unclassified),
                Syscall("getcpu", OperationType.Unclassified),
                Syscall("process_vm_readv", OperationType.Unclassified),
                Syscall("process_vm_writev", OperationType.Unclassified),
                Syscall("kcmp", OperationType.Unclassified),
                Syscall("finit_module", OperationType.Unclassified),
                Syscall("sched_setattr", OperationType.Unclassified),
                Syscall("sched_getattr", OperationType.Unclassified),
                Syscall("renameat2", OperationType.Unclassified),
                Syscall("seccomp", OperationType.Unclassified),
                Syscall("getrandom", OperationType.File),
                Syscall("memfd_create", OperationType.Unclassified),
                Syscall("kexec_file_load", OperationType.Unclassified),
                Syscall("bpf", OperationType.Unclassified),
                Syscall("execveat", OperationType.Unclassified),
                Syscall("userfaultfd", OperationType.Unclassified),
                Syscall("membarrier", OperationType.Unclassified),
                Syscall("mlock2", OperationType.Unclassified),
                Syscall("copy_file_range", OperationType.Unclassified),
                Syscall("preadv2", OperationType.Unclassified),
                Syscall("pwritev2", OperationType.Unclassified),
                Syscall("pkey_mprotect", OperationType.Unclassified),
                Syscall("pkey_alloc", OperationType.Unclassified),
                Syscall("pkey_free", OperationType.Unclassified),
                Syscall("statx", OperationType.Unclassified),
                Syscall("io_pgetevents", OperationType.Unclassified),
                Syscall("rseq", OperationType.Unclassified),
                Syscall("uretprobe", OperationType.Unclassified),
                Syscall("pidfd_send_signal", OperationType.Unclassified),
                Syscall("io_uring_setup", OperationType.Unclassified),
                Syscall("io_uring_enter", OperationType.Unclassified),
                Syscall("io_uring_register", OperationType.Unclassified),
                Syscall("open_tree", OperationType.Unclassified),
                Syscall("move_mount", OperationType.Unclassified),
                Syscall("fsopen", OperationType.File),
                Syscall("fsconfig", OperationType.File),
                Syscall("fsmount", OperationType.File),
                Syscall("fspick", OperationType.Unclassified),
                Syscall("pidfd_open", OperationType.Unclassified),
                Syscall("clone3", OperationType.ProcessManagement),
                Syscall("close_range", OperationType.Unclassified),
                Syscall("openat2", OperationType.File),
                Syscall("pidfd_getfd", OperationType.Unclassified),
                Syscall("faccessat2", OperationType.Unclassified),
                Syscall("process_madvise", OperationType.Unclassified),
                Syscall("epoll_pwait2", OperationType.Unclassified),
                Syscall("mount_setattr", OperationType.Unclassified),
                Syscall("quotactl_fd", OperationType.Unclassified),
                Syscall("landlock_create_ruleset", OperationType.Unclassified),
                Syscall("landlock_add_rule", OperationType.Unclassified),
                Syscall("landlock_restrict_self", OperationType.Unclassified),
                Syscall("memfd_secret", OperationType.Unclassified),
                Syscall("process_mrelease", OperationType.Unclassified),
                Syscall("futex_waitv", OperationType.Unclassified),
                Syscall("set_mempolicy_home_node", OperationType.Unclassified),
                Syscall("cachestat", OperationType.Unclassified),
                Syscall("fchmodat2", OperationType.Unclassified),
                Syscall("map_shadow_stack", OperationType.Unclassified),
                Syscall("futex_wake", OperationType.Unclassified),
                Syscall("futex_wait", OperationType.Unclassified),
                Syscall("futex_requeue", OperationType.Unclassified),
                Syscall("statmount", OperationType.File),
                Syscall("listmount", OperationType.File),
                Syscall("lsm_get_self_attr", OperationType.Unclassified),
                Syscall("lsm_set_self_attr", OperationType.Unclassified),
                Syscall("lsm_list_modules", OperationType.Unclassified),
                Syscall("mseal", OperationType.Unclassified),
                Syscall("setxattrat", OperationType.Unclassified),
                Syscall("getxattrat", OperationType.Unclassified),
                Syscall("listxattrat", OperationType.Unclassified),
                Syscall("removexattrat", OperationType.Unclassified),
                Syscall("open_tree_attr", OperationType.Unclassified)
        )

actual fun syscallNameToNum(name: String): Int {
        var i = 0
        while (i != x64_linux_syscall_num_to_name.size) {
                if (x64_linux_syscall_num_to_name.get(i).name == name) {
                        return i
                }
                i += 1
        }
        return -1
}

actual fun syscallNumToSyscall(num: Int): Syscall {
        return x64_linux_syscall_num_to_name.get(num)
}

actual fun getNumSyscalls(): Int {
        return x64_linux_syscall_num_to_name.size
}

actual fun getSyscallsOfType(type: OperationType): ArrayList<String> {
        var fittingSyscalls = ArrayList<String>()
        for (syscall in x64_linux_syscall_num_to_name) {
                if (syscall.type == type) {
                        fittingSyscalls.add(syscall.name)
                }
        }
        return fittingSyscalls
}

actual fun traceExecutable(
        executablePath: String,
        args: List<String>,
        timeoutSeconds: Long,
        sandbox: SandboxingOptions
): TracingResult {
        val executable = "/tmp/HackaTUM/tracer"

        val arguments = ArrayList<String>()
        println(sandbox.syscall_restrictions.size.toString())
        arguments.add(sandbox.syscall_restrictions.size.toString())
        for (sys in sandbox.syscall_restrictions) {
                arguments.add(sys.toString())
        }
        if (sandbox.syscall_restrictions_stage_2.isNotEmpty()) {
                arguments.add("-two-step")
                arguments.add(sandbox.condition.toString())
                arguments.add(sandbox.syscall_restrictions_stage_2.size.toString())
                for (sys in sandbox.syscall_restrictions_stage_2) {
                        arguments.add(sys.toString())
                }
        } else {
                arguments.add("-one-step")
        }

        arguments.add(executablePath)
        arguments.addAll(args)

        println("Executing command: $executable ${arguments.joinToString(" ")}")

        val tracingRes = executeExecutable(executable, arguments)

        // println("\n--- Execution Result ---")
        // println("Exit Code: $code")
        // println("Output:")
        // println(output)
        // println("------------------------")

        return tracingRes
}

fun main() = application {
        var tracee_path = ""
        val filePicker = JvmFilePicker { path -> tracee_path = path }

        Window(
                onCloseRequest = ::exitApplication,
                title = "aegis",
        ) { App(filePicker) }
}

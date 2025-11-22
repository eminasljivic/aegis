#include <cstdlib>
#include <errno.h>
#include <iostream>
#include "linux_compat.h"
#include <linux/filter.h>
#include <linux/seccomp.h>
#include "sandboxer.h"
#include <seccomp.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <vector>

void sandbox_current_process_seccomp(std::vector<uint32_t>& syscalls_to_restrict) {

    scmp_filter_ctx ctx = seccomp_init(SCMP_ACT_ALLOW);
    if (!ctx) {
        std::cerr << "seccomp_init failed\n";
        exit(1);
    }
    for (const auto sc : syscalls_to_restrict) {
        int rc = seccomp_rule_add(ctx, SCMP_ACT_ERRNO(EPERM), sc, 0);
        if (rc < 0) {
            std::cerr << "Failed to add rule for syscall " << sc << " (error " << rc << ")\n";
            seccomp_release(ctx);
            exit(1);
        }
    }

    if (seccomp_load(ctx) < 0) {
        std::cerr << "seccomp_load failed\n";
        seccomp_release(ctx);
        exit(1);
    }
}
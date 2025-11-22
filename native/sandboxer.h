#pragma once

#include <vector>
#include <cstdint>

void sandbox_current_process_seccomp(std::vector<uint32_t>& syscalls_to_restrict);
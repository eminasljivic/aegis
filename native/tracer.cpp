#include <sys/stat.h>
#include <sys/types.h>

#include <cstdio>
#include <cstring>
#include <fcntl.h>
#include <stdlib.h>
#include <unistd.h>

int main() {
    int shared_file = open("shared_file", O_RDWR);
    const char* msg = "test :)";
    write(shared_file, msg, strlen(msg) + 1);
    printf("Test output hehe\n");
    close(shared_file);
    return 0;
}

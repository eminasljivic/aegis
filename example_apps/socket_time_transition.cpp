#include "fcntl.h"
#include <fcntl.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

int main() {
    int fd = open("/tmp/test", O_RDWR);
    if (fd >= 0) {
        printf("file opened succesfully\n");
    } else {
        printf("file opening failed before socket\n");
    }
    write(fd, ":)", 3);
    close(fd);
    volatile int fd2 = socket(0, 0, 0);

    fd2 = open("/tmp/test2", O_RDWR);
    if (fd2 < 0) {
        printf("opening file failed after socket\n");
    } else {
        printf("opening file successfull after socket\n");
    }

    fd = open("/tmp/test", O_RDWR);

    write(fd, ":)", 3);
    close(fd);
    return 0;
}

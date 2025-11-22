#include "fcntl.h"
#include <stdio.h>
#include <unistd.h>

int main() {
  int fd = open("/tmp/test", O_RDWR);
  write(fd, ":)", 3);
  close(fd);
  return 0;
}

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/wait.h>

void* foo (void* arg){
    sleep(1);
    return NULL;
}

int main(int argc, char const *argv[]){
    int sleeptime = 1;

    for(int j = 0; j < 5; j++) {
        printf("hehe");
        int randomNum = rand() % 3;

        switch (randomNum) {
            case 0: {
                FILE* f1 = fopen("file.txt", "r");
                if(!f1) {
                    break;
                }

                sleep(sleeptime);
                const char* string = "Hallo";
                size_t size = strlen(string);

                if(fwrite(string, 1, size ,f1)){
                    fclose(f1);
                    break;
                }

                fclose(f1);
                sleep(sleeptime);
                break;
            }
            case 1: {
                FILE* f2 = fopen("file.txt", "r");
                if(!f2) {
                    break;
                }

                sleep(sleeptime);
                struct stat i;

                if(fstat(fileno(f2), &i)){
                    sleep(sleeptime);
                    fclose(f2);
                    break;
                };

                sleep(sleeptime);
                char *buf = malloc(i.st_size + 1);
                if(buf == NULL) {
                    sleep(sleeptime);
                    fclose(f2);
                    break;
                }

                sleep(sleeptime);
                if(fread(buf, 1, i.st_size, f2) != i.st_size){
                    sleep(sleeptime);
                    fclose(f2);
                    free(buf);
                    break;
                }

                free(buf);
                fclose(f2);
                sleep(sleeptime);
                break;
            }
            case 2: {
                pid_t pid = fork();
                sleep(sleeptime);

                if (pid != 0){
                    int status;
                    while (wait(&status) != -1 ){
                        sleep(sleeptime);
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    return 0;
}
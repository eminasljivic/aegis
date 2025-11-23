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
        int randomNum = rand() % 4;

        switch (randomNum) {
            case 1: {
                FILE* f1 = fopen("file.txt", "r");
                if(!f1) {
                    printf("hehe\n");

                    break;
                }

                sleep(sleeptime);
                printf("hehe\n");
                const char* string = "Hallo";
                size_t size = strlen(string);

                if(fwrite(string, 1, size ,f1)){
                    fclose(f1);
                    break;
                }

                fclose(f1);
                sleep(sleeptime);
                printf("hehe\n");
                break;
            }
            case 2: {
                FILE* f2 = fopen("file.txt", "r");
                if(!f2) {
                    printf("hehe\n");
                    break;
                }

                sleep(sleeptime);
                printf("hehe\n");
                struct stat i;

                if(fstat(fileno(f2), &i)){
                    sleep(sleeptime);
                    printf("hehe\n");
                    fclose(f2);
                    break;
                };

                sleep(sleeptime);
                printf("hehe\n");
                char *buf = malloc(i.st_size + 1);
                if(buf == NULL) {
                    sleep(sleeptime);
                    printf("hehe\n");
                    fclose(f2);
                    break;
                }

                sleep(sleeptime);
                printf("hehe\n");
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
            case 3: {
                pid_t pid = fork();
                printf("hehe\n");
                sleep(sleeptime);

                if (pid != 0){
                    int status;
                    while (wait(&status) != -1 ){
                        printf("hehe\n");
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
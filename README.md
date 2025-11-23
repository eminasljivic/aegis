# Aegis

Aegis is a lightweight syscall monitoring and sandboxing solution. It was submitted as part of the Jetbrains Challenge at HackaTUM 2025 by Dominik M. Weber, Emina Sljivic and Hanna Körkel.

## Build instructions

For JVM on Linux, just run make. For Android Native, please use IntelliJ and just hit the play button upon opening the project.


## Inspiration
In the modern days of adamant IP protection, black-box apps without source code are commonplace. Spinning up a VM anytime you want to run a semi-trusted app is expensive and cumbersome - we need a lightweight solution protecting us from any malicious intent while allowing us to inspect the behaviour of the app.

## What it does
Aegis allows you to pick an untrusted app, write a sandboxing policy for it and then execute it safely, all while getting deep insights into the inner workings of the application. Inspired by recent papers, we even added a multi-stage temporally specialised syscall filtering option, removing standing privileges waiting to be exploited by malicious actors. Aegis runs on both Linux and Android with extensibility for various other platforms due to its modular design, leveraging Kotlin Multiplatform.

## How we built it
The syscall tracing is done using ptrace. The Kotlin app deploys a C++ helper which then forks off the child process to be traced and starts monitoring its syscalls. These are reported back to the original app, where the information is processed and visualised for the user. 
Syscall filtering is done in two ways. The primary way is using seccomp, the Linux kernel syscal filter. For the temporal specialisation, the ptracing helper monitors the syscalls itself and, upon hitting a condition specified in a policy file, further restricts the semi-trusted app's abilities by killing it upon misbehaviour.

## Challenges we ran into
Getting everything to run on Android proved to be a challenge due to the tight security options already being in place there. 

## Accomplishments that we're proud of
Getting a halfway decent UI to work. We are not web-devs!

## What we learned
Kotlin is an interesting language with lots of cool features.

## What's next for Aegis
The untold depths of a Github repository.

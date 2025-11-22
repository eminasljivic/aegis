all:
	#cd native && make
	cd example_apps && make
	./gradlew run

setup:
	mkdir -p /tmp/HackaTUM
	ls /tmp/HackaTUM/program_out_fifo || mkfifo /tmp/HackaTUM/program_out_fifo
	ls /tmp/HackaTUM/program_err_fifo || mkfifo /tmp/HackaTUM/program_err_fifo
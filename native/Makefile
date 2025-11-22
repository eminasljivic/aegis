all:
	g++ tracer.cpp sandboxer.cpp -lseccomp -o tracer
	cp tracer /tmp/HackaTUM/tracer
	ls /tmp/HackaTUM/program_out_fifo || mkfifo /tmp/HackaTUM/program_out_fifo
	ls /tmp/HackaTUM/program_err_fifo || mkfifo /tmp/HackaTUM/program_err_fifo
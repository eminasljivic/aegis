all:
	cd native && make
	./gradlew run

setup:
	mkdir -p /tmp/HackaTUM
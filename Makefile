all:
	cd native && make
	cd example_apps && make
	./gradlew run

setup:
	mkdir -p /tmp/HackaTUM
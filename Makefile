SHELL := /bin/sh

.PHONY: all server build gen-data


all: build server

server:
	@echo "==> Running server script..."
	sh ./.server/run.sh

build:
	@echo "==> Copying files to plugins..."
	./gradlew copyToPlugins

gen-data:
	@echo "==> Generating binary test data..."
	python ./test_data/gen_bin.py
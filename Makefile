SHELL := /bin/sh

.PHONY: all server build gen-data palette clear-world gen-minecraft-data


all: clear-world build server

server:
	@echo "==> Running server script..."
	sh ./.server/run.sh

build:
	@echo "==> Copying files to plugins..."
	./gradlew copyToPlugins

gen-data:
	@echo "==> Generating binary test data..."
	python ./utils/binary_generator.py

palette:
	@echo "==> Generating palette..."
	python ./utils/download_sprites.py ./test_data/concrete.txt -o ./test_data/blocks
	go run ./utils/average_color.go ./test_data/blocks > ./test_data/blocks/average.csv

clear-world:
	@echo "==> Removing world..."
	rm -rf ./server/world

gen-minecraft-data:
	@echo "==> Generating minecraft data..."
	java -DbundlerMainClass=net.minecraft.data.Main -jar ./.server/paper-26.1.2-65.jar --reports

gen-atlas:
	@echo "==> Generating atlas..."
	go run ./utils/texture_atlas.go \
	-src=./test_data/blocks/matched_textures \
	-atlas="./test_data/output/blocks.png" \
	-csv="./test_data/output/coords.csv"
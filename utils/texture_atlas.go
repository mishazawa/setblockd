package main

import (
	"bufio"
	"encoding/csv"
	"flag"
	"fmt"
	"image"
	"image/png"
	"math"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

func main() {
	keysFile := flag.String("keys", "./test_data/minecraft_block_keys.txt", "Path to the keys text file")
	blockFolder := flag.String("src", "./input_blocks_dir", "Path to the raw folder containing all PNG textures")
	outputAtlas := flag.String("atlas", "texture_atlas.png", "Filename for the output compiled atlas image")
	outputCSV := flag.String("csv", "atlas_map.csv", "Filename for the output CSV coordinate map")
	// Added padding flag
	padding := flag.Int("padding", 2, "Padding in pixels around each sprite to prevent bleeding")

	flag.Parse()

	file, err := os.Open(*keysFile)
	if err != nil {
		fmt.Printf("Error opening keys file: %v\n", err)
		return
	}
	defer file.Close()

	var files []string
	scanner := bufio.NewScanner(file)

	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}

		name := strings.TrimPrefix(line, "minecraft:")
		imgPath := filepath.Join(*blockFolder, name+".png")

		if _, err := os.Stat(imgPath); err == nil {
			files = append(files, imgPath)
		}
	}

	if err := scanner.Err(); err != nil {
		fmt.Printf("Error reading keys file: %v\n", err)
		return
	}

	if len(files) == 0 {
		fmt.Printf("No matching .png files found based on '%s'\n", *keysFile)
		return
	}

	totalFiles := len(files)
	fmt.Printf("Found %d images. Processing...\n", totalFiles)

	firstFile, err := os.Open(files[0])
	if err != nil {
		fmt.Printf("Error opening first file: %v\n", err)
		return
	}
	firstConfig, _, err := image.DecodeConfig(firstFile)
	firstFile.Close()
	if err != nil {
		fmt.Printf("Error decoding first file config: %v\n", err)
		return
	}

	spriteW, spriteH := firstConfig.Width, firstConfig.Height

	columns := int(math.Ceil(math.Sqrt(float64(totalFiles))))
	rows := int(math.Ceil(float64(totalFiles) / float64(columns)))

	// Calculate slot dimensions including padding on all sides
	slotW := spriteW + (*padding * 2)
	slotH := spriteH + (*padding * 2)

	atlasW := columns * slotW
	atlasH := rows * slotH

	fmt.Printf("Creating %dx%d atlas (%d cols x %d rows) with padding %d...\n", atlasW, atlasH, columns, rows, *padding)

	atlasBounds := image.Rect(0, 0, atlasW, atlasH)
	atlas := image.NewRGBA(atlasBounds)

	csvFile, err := os.Create(*outputCSV)
	if err != nil {
		fmt.Printf("Error creating CSV file: %v\n", err)
		return
	}
	defer csvFile.Close()

	writer := csv.NewWriter(csvFile)
	defer writer.Flush()

	if err := writer.Write([]string{"name", "u0", "v0", "u1", "v1"}); err != nil {
		fmt.Printf("Error writing CSV header: %v\n", err)
		return
	}

	fatlasW := float32(atlasW)
	fatlasH := float32(atlasH)
	fspriteW := float32(spriteW)
	fspriteH := float32(spriteH)

	for index, filePath := range files {
		col := index % columns
		row := index / columns

		// Determine the top-left coordinate of the slotted cell
		cellX := col * slotW
		cellY := row * slotH

		// The actual sprite is offset within the cell by the padding amount
		spriteX := cellX + *padding
		spriteY := cellY + *padding

		fspriteX := float32(spriteX)
		fspriteY := float32(spriteY)

		// UV Mapping coordinates point precisely to the true sprite boundaries
		uMin := fspriteX / fatlasW
		uMax := (fspriteX + fspriteW) / fatlasW
		vMin := (fatlasH - (fspriteY + fspriteH)) / fatlasH
		vMax := (fatlasH - fspriteY) / fatlasH

		f, err := os.Open(filePath)
		if err != nil {
			fmt.Printf("Skipping %s due to error: %v\n", filePath, err)
			continue
		}

		img, _, err := image.Decode(f)
		f.Close()
		if err != nil {
			fmt.Printf("Skipping %s due to decode error: %v\n", filePath, err)
			continue
		}

		// --- 1. Render the padded/extruded edges ---
		// We loop over the entire cell slot and clamp pixel lookups to the sprite bounds
		for sy := 0; sy < slotH; sy++ {
			for sx := 0; sx < slotW; sx++ {
				// Translate slot space back to original sprite space coordinates
				origX := sx - *padding
				origY := sy - *padding

				// Clamp coordinates to edge pixels to stretch them into the padding zone
				clampX := origX
				if clampX < 0 {
					clampX = 0
				} else if clampX >= spriteW {
					clampX = spriteW - 1
				}

				clampY := origY
				if clampY < 0 {
					clampY = 0
				} else if clampY >= spriteH {
					clampY = spriteH - 1
				}

				// Copy pixel color to the destination atlas canvas
				color := img.At(img.Bounds().Min.X+clampX, img.Bounds().Min.Y+clampY)
				atlas.Set(cellX+sx, cellY+sy, color)
			}
		}

		// --- 2. Write down the true dimensions to CSV map ---
		baseName := filepath.Base(filePath)
		nameWithoutExt := baseName[:len(baseName)-len(filepath.Ext(baseName))]

		record := []string{
			nameWithoutExt,
			strconv.FormatFloat(float64(uMin), 'f', 6, 64),
			strconv.FormatFloat(float64(vMin), 'f', 6, 64),
			strconv.FormatFloat(float64(uMax), 'f', 6, 64),
			strconv.FormatFloat(float64(vMax), 'f', 6, 64),
		}
		if err := writer.Write(record); err != nil {
			fmt.Printf("Error writing CSV record: %v\n", err)
			return
		}
	}

	outImageFile, err := os.Create(*outputAtlas)
	if err != nil {
		fmt.Printf("Error creating atlas image file: %v\n", err)
		return
	}
	defer outImageFile.Close()

	if err := png.Encode(outImageFile, atlas); err != nil {
		fmt.Printf("Error encoding output PNG: %v\n", err)
		return
	}

	fmt.Println("Success!")
	fmt.Printf("-> Generated: %s\n", *outputAtlas)
	fmt.Printf("-> Generated: %s\n", *outputCSV)
}
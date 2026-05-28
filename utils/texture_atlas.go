package main

import (
	"encoding/csv"
	"flag"
	"fmt"
	"image"
	"image/draw"
	"image/png"
	"math"
	"os"
	"path/filepath"
	"sort"
	"strconv"
)

func main() {
	// 1. Define command-line flags with sensible defaults
	sourceFolder := flag.String("src", "matched_textures", "Path to the folder containing PNG textures")
	outputAtlas := flag.String("atlas", "texture_atlas.png", "Filename for the output compiled atlas image")
	outputCSV := flag.String("csv", "atlas_map.csv", "Filename for the output CSV coordinate map")

	// Parse the flags passed by the user
	flag.Parse()

	// 2. Gather and sort all PNG files
	files, err := filepath.Glob(filepath.Join(*sourceFolder, "*.png"))
	if err != nil {
		fmt.Printf("Error reading source folder: %v\n", err)
		return
	}
	if len(files) == 0 {
		fmt.Printf("No .png files found in '%s'\n", *sourceFolder)
		return
	}
	sort.Strings(files)

	totalFiles := len(files)
	fmt.Printf("Found %d images. Processing...\n", totalFiles)

	// 3. Open the first image to determine uniform sprite dimensions
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

	// 4. Calculate grid layout (Columns x Rows) targeting a square aspect ratio
	columns := int(math.Ceil(math.Sqrt(float64(totalFiles))))
	rows := int(math.Ceil(float64(totalFiles) / float64(columns)))

	atlasW := columns * spriteW
	atlasH := rows * spriteH

	fmt.Printf("Creating %dx%d atlas (%d cols x %d rows)...\n", atlasW, atlasH, columns, rows)

	// 5. Create the blank destination canvas (RGBA supporting transparency)
	atlasBounds := image.Rect(0, 0, atlasW, atlasH)
	atlas := image.NewRGBA(atlasBounds)

	// 6. Setup the CSV file writer
	csvFile, err := os.Create(*outputCSV)
	if err != nil {
		fmt.Printf("Error creating CSV file: %v\n", err)
		return
	}
	defer csvFile.Close()

	writer := csv.NewWriter(csvFile)
	defer writer.Flush()

	// Write CSV Header
	if err := writer.Write([]string{"name", "x", "y"}); err != nil {
		fmt.Printf("Error writing CSV header: %v\n", err)
		return
	}

	// 7. Iterate through images, draw them onto the atlas, and map coords
	for index, filePath := range files {
		col := index % columns
		row := index / columns

		xOffset := col * spriteW
		yOffset := row * spriteH

		// Open individual sprite
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

		// Calculate destination rectangle bounds for this sprite on the atlas
		dp := image.Pt(xOffset, yOffset)
		sr := image.Rect(0, 0, spriteW, spriteH)
		dr := image.Rectangle{Min: dp, Max: dp.Add(sr.Size())}

		// Composite the sprite onto the atlas canvas
		draw.Draw(atlas, dr, img, image.Point{}, draw.Over)

		// Strip directory paths and ".png" extension for clean CSV keys
		baseName := filepath.Base(filePath)
		nameWithoutExt := baseName[:len(baseName)-len(filepath.Ext(baseName))]

		// Log entry to CSV
		record := []string{
			nameWithoutExt,
			strconv.Itoa(xOffset),
			strconv.Itoa(yOffset),
		}
		if err := writer.Write(record); err != nil {
			fmt.Printf("Error writing CSV record: %v\n", err)
			return
		}
	}

	// 8. Save the final compiled texture atlas to disk
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
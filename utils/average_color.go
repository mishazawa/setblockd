package main

import (
	"fmt"
	"image"
	_ "image/jpeg" 
	_ "image/png"  
	"os"
	"path/filepath"
	"strings"
)

func getAverageHex(filePath string) (string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return "", err
	}
	defer file.Close()

	img, _, err := image.Decode(file)
	if err != nil {
		return "", err
	}

	bounds := img.Bounds()
	var totalR, totalG, totalB uint64

	for y := bounds.Min.Y; y < bounds.Max.Y; y++ {
		for x := bounds.Min.X; x < bounds.Max.X; x++ {
			r, g, b, _ := img.At(x, y).RGBA()
			totalR += uint64(r)
			totalG += uint64(g)
			totalB += uint64(b)
		}
	}

	totalPixels := uint64(bounds.Dx() * bounds.Dy())
	if totalPixels == 0 {
		return "#000000", nil
	}

	avgR := (totalR / totalPixels) >> 8
	avgG := (totalG / totalPixels) >> 8
	avgB := (totalB / totalPixels) >> 8

	return fmt.Sprintf("#%02X%02X%02X", avgR, avgG, avgB), nil
}

func main() {
	dirPath := "."
	if len(os.Args) > 1 {
		dirPath = os.Args[1]
	}

	files, err := os.ReadDir(dirPath)
	if err != nil {
		fmt.Printf("Error reading directory: %v\n", err)
		return
	}

	fmt.Println("name,hex")

	validExtensions := map[string]bool{
		".jpg":  true,
		".jpeg": true,
		".png":  true,
		".gif":  true,
	}

	for _, file := range files {
		if file.IsDir() {
			continue
		}

		ext := filepath.Ext(file.Name())
		if !validExtensions[strings.ToLower(ext)] {
			continue
		}

		nameWithoutExt := strings.TrimSuffix(file.Name(), ext)

		filePath := filepath.Join(dirPath, file.Name())
		hexColor, err := getAverageHex(filePath)
		if err != nil {
			continue
		}

		fmt.Printf("%s,%s\n", nameWithoutExt, hexColor)
	}
}
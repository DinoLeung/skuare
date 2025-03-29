package main

import (
	"encoding/csv"
	"fmt"
	"os"
	"strconv"
	"strings"
)

func main() {
	// Open the CSV file
	file, err := os.Open("home_time_city.csv")
	if err != nil {
		panic(err)
	}
	defer file.Close()

	// Read the CSV file
	reader := csv.NewReader(file)
	records, err := reader.ReadAll()
	if err != nil {
		panic(err)
	}

	// Skip the header and process each line
	var kotlinMap strings.Builder
	kotlinMap.WriteString("val homeTimezones: Map<Int, HomeTimezone> = mapOf(\n")

	for i, record := range records {
		if i == 0 {
			continue // Skip the header
		}

		id, _ := strconv.Atoi(record[0])                // ID
		offset, _ := strconv.ParseFloat(record[1], 64)  // Offset
		dstDiff, _ := strconv.ParseFloat(record[2], 64) // DST Diff
		dstRule, _ := strconv.Atoi(record[3])           // DST Rule
		cityName := record[4]                           // Name
		timeZone := record[5]                           // Timezone

		kotlinMap.WriteString(fmt.Sprintf(
			"    %d to HomeTimezone(\n        timeZone = \"%s\",\n        cityName = \"%s\",\n        identifier = %d,\n        offset = %.1f,\n        dstDiff = %.1f,\n        dstRules = %d\n    ),\n",
			id, timeZone, cityName, id, offset, dstDiff, dstRule,
		))
	}

	// Remove the trailing comma and close the map
	kotlinOutput := strings.TrimSuffix(kotlinMap.String(), ",\n") + "\n)"

	// Write the Kotlin map to a file
	outputFile, err := os.Create("homeCities.kt")
	if err != nil {
		panic(err)
	}
	defer outputFile.Close()

	_, err = outputFile.WriteString(kotlinOutput)
	if err != nil {
		panic(err)
	}

	fmt.Println("Kotlin map has been written to homeCities.kt")
}

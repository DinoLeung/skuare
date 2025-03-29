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
    file, err := os.Open("world_time_city.csv")
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
    kotlinMap.WriteString("val worldTimezones: Map<Int, WorldTimezone> = mapOf(\n")

    for i, record := range records {
        if i == 0 {
            continue // Skip the header
        }

        id, _ := strconv.Atoi(record[0])                  // ID
        offset, _ := strconv.ParseFloat(record[1], 64)    // Offset
        dstDiff, _ := strconv.ParseFloat(record[2], 64)   // DST Diff
        dstRule, _ := strconv.Atoi(record[3])             // DST Rule
        cityName := record[4]                             // Name
        country := record[5]                              // Country
        city := record[6]                                 // City
        longitude, _ := strconv.ParseFloat(record[7], 64) // Longitude
        latitude, _ := strconv.ParseFloat(record[8], 64)  // Latitude

        kotlinMap.WriteString(fmt.Sprintf(
            "    %d to WorldTimezone(\n        country = \"%s\",\n        city = \"%s\",\n        cityName = \"%s\",\n        identifier = %d,\n        offset = %.1f,\n        dstDiff = %.1f,\n        dstRules = %d,\n        longitude = %.5f,\n        latitude = %.5f\n    ),\n",
            id, country, city, cityName, id, offset, dstDiff, dstRule, longitude, latitude,
        ))
    }

    // Remove the trailing comma and close the map
    kotlinOutput := strings.TrimSuffix(kotlinMap.String(), ",\n") + "\n)"

    // Write the Kotlin map to a file
    outputFile, err := os.Create("worldCities.kt")
    if err != nil {
        panic(err)
    }
    defer outputFile.Close()

    _, err = outputFile.WriteString(kotlinOutput)
    if err != nil {
        panic(err)
    }

    fmt.Println("Kotlin map has been written to worldCities.kt")
}

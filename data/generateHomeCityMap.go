package main

// Run together with iataCityCodes.go: go run generateHomeCityMap.go iataCityCodes.go

import (
	"bufio"
	"bytes"
	"encoding/csv"
	"fmt"
	"os"
	"strconv"
	"strings"
)

type homeCity struct {
	id       int
	offset   float64
	dstDiff  float64
	dstRule  int
	cityName string
	cityCode string
	timeZone string
}

func main() {
	if err := run(); err != nil {
		fmt.Fprintln(os.Stderr, "generate home city map:", err)
		os.Exit(1)
	}
}

func run() error {
	cities, err := readCities("home_time_city.csv")
	if err != nil {
		return err
	}

	ianaData, err := download(ianaDataURL)
	if err != nil {
		return fmt.Errorf("download latest IANA timezone data: %w", err)
	}
	iataCities, err := downloadIATACities()
	if err != nil {
		return err
	}
	assignCityCodes(cities, iataCities, parseTimeZoneLinks(ianaData))

	if err := os.WriteFile("homeCities.kt", []byte(generateKotlin(cities)), 0o644); err != nil {
		return fmt.Errorf("write homeCities.kt: %w", err)
	}
	fmt.Println("Kotlin map has been written to homeCities.kt")
	return nil
}

func readCities(path string) ([]*homeCity, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("open %s: %w", path, err)
	}
	defer file.Close()

	records, err := csv.NewReader(file).ReadAll()
	if err != nil {
		return nil, fmt.Errorf("read %s: %w", path, err)
	}

	cities := make([]*homeCity, 0, len(records))
	for i, record := range records {
		if i == 0 {
			continue // Skip the header
		}

		id, _ := strconv.Atoi(record[0])                // ID
		offset, _ := strconv.ParseFloat(record[1], 64)  // Offset
		dstDiff, _ := strconv.ParseFloat(record[2], 64) // DST Diff
		dstRule, _ := strconv.Atoi(record[3])           // DST Rule
		cities = append(cities, &homeCity{
			id:       id,
			offset:   offset,
			dstDiff:  dstDiff,
			dstRule:  dstRule,
			cityName: record[4], // Name
			timeZone: record[5], // Timezone
		})
	}
	return cities, nil
}

// parseTimeZoneLinks maps each IANA link to the zone it points to, e.g. Asia/Calcutta to
// Asia/Kolkata, from the "L target link" lines of tzdata.zi.
func parseTimeZoneLinks(data []byte) map[string]string {
	links := map[string]string{}
	scanner := bufio.NewScanner(bytes.NewReader(data))
	for scanner.Scan() {
		if fields := strings.Fields(scanner.Text()); len(fields) >= 3 && fields[0] == "L" {
			links[fields[2]] = fields[1]
		}
	}
	return links
}

// assignCityCodes gives each city the IATA city code of a city with the same name in the same
// time zone, comparing zones after resolving links (Asia/Calcutta is Asia/Kolkata). With more than
// one match the most important city (highest page rank) wins. Cities without a match get null, e.g.
// zones that aren't cities like US/Eastern or Etc/GMT+5.
func assignCityCodes(cities []*homeCity, iataCities []*iataCity, links map[string]string) {
	canonical := func(timeZone string) string {
		if target, ok := links[timeZone]; ok {
			return target
		}
		return timeZone
	}

	var missing []string
	for _, city := range cities {
		bestRank := -1.0
		for _, iata := range iataCities {
			if canonical(iata.timeZone) != canonical(city.timeZone) || !iata.hasName(city.cityName) {
				continue
			}
			if iata.pageRank > bestRank {
				bestRank = iata.pageRank
				city.cityCode = iata.code
			}
		}
		if city.cityCode == "" {
			missing = append(missing, fmt.Sprintf("%d %s (%s)", city.id, city.cityName, city.timeZone))
		}
	}
	printMissingCityCodes(missing, len(cities))
}

func generateKotlin(cities []*homeCity) string {
	var kotlinMap strings.Builder
	kotlinMap.WriteString("package xyz.d1n0.lib.constant\n\n")
	kotlinMap.WriteString("import xyz.d1n0.lib.model.HomeTimezone\n\n")
	kotlinMap.WriteString("val HomeTimezoneData: Map<Int, HomeTimezone> = mapOf(\n")

	for _, city := range cities {
		fmt.Fprintf(
			&kotlinMap,
			"\t%d to HomeTimezone(\n\t\ttimeZone = \"%s\",\n\t\tcityName = \"%s\",\n\t\tcityCode = %s,\n\t\tidentifier = %d,\n\t\toffset = %s,\n\t\tdstDiff = %s,\n\t\tdstRules = %d\n\t),\n",
			city.id,
			kotlinString(city.timeZone),
			kotlinString(city.cityName),
			kotlinNullableString(city.cityCode),
			city.id,
			kotlinDouble(city.offset),
			kotlinDouble(city.dstDiff),
			city.dstRule,
		)
	}

	// Remove the trailing comma and close the map
	return strings.TrimSuffix(kotlinMap.String(), ",\n") + "\n)"
}

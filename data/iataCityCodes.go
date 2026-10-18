// Shared by the city map generators, run each one together with this file:
//
//	go run generateHomeCityMap.go iataCityCodes.go
//	go run generateWorldCitymap.go iataCityCodes.go
package main

import (
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	// OpenTravelData points of reference, the open IATA code data set, '^' separated.
	optdURL         = "https://raw.githubusercontent.com/opentraveldata/opentraveldata/master/opentraveldata/optd_por_public.csv"
	ianaDataURL     = "https://data.iana.org/time-zones/tzdb/tzdata.zi"
	maxDownloadSize = 128 << 20
)

var httpClient = &http.Client{Timeout: 10 * time.Minute}

// iataCity is a city with an IATA city code, e.g. TYO for Tokyo. Airports aren't included, so
// a city gets its city code even when its airports have other codes (HND, NRT).
type iataCity struct {
	code      string
	names     map[string]struct{} // normalized, see normalizeName
	latitude  float64
	longitude float64
	timeZone  string
	pageRank  float64
}

func (c *iataCity) hasName(name string) bool {
	_, ok := c.names[normalizeName(name)]
	return ok
}

// downloadIATACities downloads the cities with a current IATA city code from OpenTravelData.
func downloadIATACities() ([]*iataCity, error) {
	data, err := download(optdURL)
	if err != nil {
		return nil, fmt.Errorf("download OpenTravelData: %w", err)
	}

	lines := strings.Split(string(data), "\n")
	if len(lines) < 2 {
		return nil, errors.New("OpenTravelData contains no records")
	}
	columns := map[string]int{}
	for i, name := range strings.Split(lines[0], "^") {
		columns[name] = i
	}
	for _, name := range []string{"iata_code", "name", "asciiname", "latitude", "longitude", "page_rank", "date_until", "timezone", "location_type", "alt_name_section"} {
		if _, ok := columns[name]; !ok {
			return nil, fmt.Errorf("OpenTravelData has no %s column", name)
		}
	}

	var cities []*iataCity
	for _, line := range lines[1:] {
		fields := strings.Split(line, "^")
		if len(fields) < len(columns) {
			continue
		}
		field := func(name string) string { return fields[columns[name]] }
		// C, CA, CR, … are cities, A, R, … without C are airports, stations and so on.
		// Codes with an end date are no longer in use.
		if len(field("iata_code")) != 3 || !strings.Contains(field("location_type"), "C") || field("date_until") != "" {
			continue
		}

		latitude, err := strconv.ParseFloat(field("latitude"), 64)
		if err != nil {
			continue
		}
		longitude, err := strconv.ParseFloat(field("longitude"), 64)
		if err != nil {
			continue
		}
		pageRank, _ := strconv.ParseFloat(field("page_rank"), 64)

		city := &iataCity{
			code:      field("iata_code"),
			names:     map[string]struct{}{},
			latitude:  latitude,
			longitude: longitude,
			timeZone:  field("timezone"),
			pageRank:  pageRank,
		}
		city.addName(field("name"))
		city.addName(field("asciiname"))
		// lang|name|flags=lang|name|flags=…
		for _, altName := range strings.Split(field("alt_name_section"), "=") {
			if parts := strings.Split(altName, "|"); len(parts) >= 2 {
				city.addName(parts[1])
			}
		}
		cities = append(cities, city)
	}
	if len(cities) == 0 {
		return nil, errors.New("OpenTravelData contains no cities with an IATA code")
	}
	return cities, nil
}

func (c *iataCity) addName(name string) {
	if normalized := normalizeName(name); normalized != "" {
		c.names[normalized] = struct{}{}
	}
}

// normalizeName keeps only the letters and digits, uppercased, so "St. John's", "ST JOHNS" and
// "Saint-Petersburg" / "SAINT PETERSBURG" compare equal. Names with non-ASCII letters are
// dropped, the ASCII spellings are in the data set too.
func normalizeName(name string) string {
	var normalized strings.Builder
	for _, r := range strings.ToUpper(name) {
		switch {
		case r > 0x7F:
			return ""
		case r >= 'A' && r <= 'Z', r >= '0' && r <= '9':
			normalized.WriteRune(r)
		}
	}
	return normalized.String()
}

func download(url string) ([]byte, error) {
	request, err := http.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	request.Header.Set("Accept", "application/vnd.github+json, text/plain, application/octet-stream")
	request.Header.Set("User-Agent", "skuare-timezone-generator")
	if token := os.Getenv("GITHUB_TOKEN"); token != "" &&
		(request.URL.Hostname() == "github.com" || request.URL.Hostname() == "api.github.com") {
		request.Header.Set("Authorization", "Bearer "+token)
	}

	response, err := httpClient.Do(request)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return nil, fmt.Errorf("GET %s returned %s", url, response.Status)
	}

	data, err := io.ReadAll(io.LimitReader(response.Body, maxDownloadSize+1))
	if err != nil {
		return nil, err
	}
	if len(data) > maxDownloadSize {
		return nil, fmt.Errorf("GET %s exceeded the %d-byte download limit", url, maxDownloadSize)
	}
	return data, nil
}

// printMissingCityCodes lists the cities that got no IATA city code, for review.
func printMissingCityCodes(missing []string, total int) {
	fmt.Fprintf(os.Stderr, "%d of %d cities have no IATA city code:\n", len(missing), total)
	for _, city := range missing {
		fmt.Fprintf(os.Stderr, "  %s\n", city)
	}
}

// kotlinDouble formats a value the way Kotlin prints a Double, e.g. 5.0, 5.5, 12.75.
func kotlinDouble(value float64) string {
	formatted := strconv.FormatFloat(value, 'f', -1, 64)
	if !strings.Contains(formatted, ".") {
		formatted += ".0"
	}
	return formatted
}

// kotlinNullableString formats a String? value, an empty value becomes null.
func kotlinNullableString(value string) string {
	if value == "" {
		return "null"
	}
	return "\"" + kotlinString(value) + "\""
}

func kotlinString(value string) string {
	replacer := strings.NewReplacer(
		"\\", "\\\\",
		"\"", "\\\"",
		"$", "\\$",
		"\n", "\\n",
		"\r", "\\r",
		"\t", "\\t",
	)
	return replacer.Replace(value)
}

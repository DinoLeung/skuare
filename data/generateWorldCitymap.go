package main

import (
	"archive/zip"
	"bufio"
	"bytes"
	"crypto/sha256"
	"encoding/csv"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math"
	"os"
	"strconv"
	"strings"
)

// Run together with iataCityCodes.go: go run generateWorldCitymap.go iataCityCodes.go

const (
	boundaryReleaseURL = "https://api.github.com/repos/evansiroky/timezone-boundary-builder/releases/latest"
	boundaryAssetName  = "timezones-with-oceans.geojson.zip"
	maxGeoJSONSize     = 512 << 20
	// a city code is only taken from a city with the same name within this distance
	maxCityCodeDistanceKm = 100
)

type cityRecord struct {
	id        int
	offset    float64
	dstDiff   float64
	dstRule   int
	cityName  string
	cityCode  string
	country   string
	city      string
	longitude float64
	latitude  float64
	timeZone  string
}

type githubAsset struct {
	Name               string `json:"name"`
	Digest             string `json:"digest"`
	BrowserDownloadURL string `json:"browser_download_url"`
}

type githubRelease struct {
	TagName string        `json:"tag_name"`
	Assets  []githubAsset `json:"assets"`
}

type geoJSONFeature struct {
	Properties struct {
		TimeZone string `json:"tzid"`
	} `json:"properties"`
	Geometry struct {
		Type        string          `json:"type"`
		Coordinates json.RawMessage `json:"coordinates"`
	} `json:"geometry"`
}

type position []float64
type ring []position
type polygon []ring

func main() {
	if err := run(); err != nil {
		fmt.Fprintln(os.Stderr, "generate world city map:", err)
		os.Exit(1)
	}
}

func run() error {
	cities, err := readCities("world_time_city.csv")
	if err != nil {
		return err
	}

	ianaData, err := download(ianaDataURL)
	if err != nil {
		return fmt.Errorf("download latest IANA timezone data: %w", err)
	}
	ianaVersion, validTimeZones, err := parseIANAData(ianaData)
	if err != nil {
		return err
	}

	release, err := getLatestBoundaryRelease()
	if err != nil {
		return err
	}
	asset, err := findBoundaryAsset(release)
	if err != nil {
		return err
	}
	if release.TagName != ianaVersion {
		fmt.Fprintf(
			os.Stderr,
			"warning: latest timezone boundaries are %s, while latest IANA data is %s\n",
			release.TagName,
			ianaVersion,
		)
	}

	boundaryArchive, err := download(asset.BrowserDownloadURL)
	if err != nil {
		return fmt.Errorf("download timezone boundaries: %w", err)
	}
	if err := verifyDigest(boundaryArchive, asset.Digest); err != nil {
		return err
	}
	if err := resolveTimeZones(boundaryArchive, cities); err != nil {
		return err
	}

	for _, city := range cities {
		if _, ok := validTimeZones[city.timeZone]; !ok {
			return fmt.Errorf(
				"timezone %q resolved for %s, %s is not present in IANA %s",
				city.timeZone,
				city.city,
				city.country,
				ianaVersion,
			)
		}
	}

	iataCities, err := downloadIATACities()
	if err != nil {
		return err
	}
	assignCityCodes(cities, iataCities)

	output := generateKotlin(cities, ianaVersion, release.TagName)
	if err := os.WriteFile("worldCities.kt", []byte(output), 0o644); err != nil {
		return fmt.Errorf("write worldCities.kt: %w", err)
	}

	fmt.Printf(
		"Kotlin map with %d IANA %s timezone IDs has been written to worldCities.kt\n",
		len(cities),
		ianaVersion,
	)
	return nil
}

func readCities(path string) ([]*cityRecord, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("open %s: %w", path, err)
	}
	defer file.Close()

	records, err := csv.NewReader(file).ReadAll()
	if err != nil {
		return nil, fmt.Errorf("read %s: %w", path, err)
	}
	if len(records) < 2 {
		return nil, fmt.Errorf("%s contains no city records", path)
	}

	cities := make([]*cityRecord, 0, len(records)-1)
	for row, record := range records[1:] {
		line := row + 2
		if len(record) != 9 {
			return nil, fmt.Errorf("%s:%d: expected 9 columns, got %d", path, line, len(record))
		}

		id, err := parseInt(record[0], path, line, "ID")
		if err != nil {
			return nil, err
		}
		offset, err := parseFloat(record[1], path, line, "Offset")
		if err != nil {
			return nil, err
		}
		dstDiff, err := parseFloat(record[2], path, line, "DST Diff")
		if err != nil {
			return nil, err
		}
		dstRule, err := parseInt(record[3], path, line, "DST Rule")
		if err != nil {
			return nil, err
		}
		longitude, err := parseFloat(record[7], path, line, "Longitude")
		if err != nil {
			return nil, err
		}
		latitude, err := parseFloat(record[8], path, line, "Latitude")
		if err != nil {
			return nil, err
		}
		if longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90 {
			return nil, fmt.Errorf("%s:%d: invalid coordinates %.6f, %.6f", path, line, latitude, longitude)
		}

		city := &cityRecord{
			id:        id,
			offset:    offset,
			dstDiff:   dstDiff,
			dstRule:   dstRule,
			cityName:  record[4],
			country:   record[5],
			city:      record[6],
			longitude: longitude,
			latitude:  latitude,
		}
		if id == 0 {
			city.timeZone = "Etc/UTC"
		}
		cities = append(cities, city)
	}

	return cities, nil
}

func parseInt(value, path string, line int, column string) (int, error) {
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return 0, fmt.Errorf("%s:%d: invalid %s %q: %w", path, line, column, value, err)
	}
	return parsed, nil
}

func parseFloat(value, path string, line int, column string) (float64, error) {
	parsed, err := strconv.ParseFloat(value, 64)
	if err != nil {
		return 0, fmt.Errorf("%s:%d: invalid %s %q: %w", path, line, column, value, err)
	}
	return parsed, nil
}

func getLatestBoundaryRelease() (githubRelease, error) {
	data, err := download(boundaryReleaseURL)
	if err != nil {
		return githubRelease{}, fmt.Errorf("download latest timezone boundary release metadata: %w", err)
	}

	var release githubRelease
	if err := json.Unmarshal(data, &release); err != nil {
		return githubRelease{}, fmt.Errorf("decode latest timezone boundary release metadata: %w", err)
	}
	if release.TagName == "" {
		return githubRelease{}, errors.New("latest timezone boundary release has no tag")
	}
	return release, nil
}

func findBoundaryAsset(release githubRelease) (githubAsset, error) {
	for _, asset := range release.Assets {
		if asset.Name == boundaryAssetName {
			return asset, nil
		}
	}
	return githubAsset{}, fmt.Errorf("release %s does not contain %s", release.TagName, boundaryAssetName)
}

func parseIANAData(data []byte) (string, map[string]struct{}, error) {
	scanner := bufio.NewScanner(bytes.NewReader(data))
	version := ""
	timeZones := make(map[string]struct{})
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if strings.HasPrefix(line, "# version ") {
			version = strings.TrimSpace(strings.TrimPrefix(line, "# version "))
			continue
		}
		fields := strings.Fields(line)
		if len(fields) >= 2 && fields[0] == "Z" {
			timeZones[fields[1]] = struct{}{}
		} else if len(fields) >= 3 && fields[0] == "L" {
			timeZones[fields[2]] = struct{}{}
		}
	}
	if err := scanner.Err(); err != nil {
		return "", nil, fmt.Errorf("read IANA timezone data: %w", err)
	}
	if version == "" || len(timeZones) == 0 {
		return "", nil, errors.New("IANA timezone data is missing its version or zone definitions")
	}
	return version, timeZones, nil
}

func verifyDigest(data []byte, digest string) error {
	if digest == "" {
		return nil
	}
	algorithm, expected, ok := strings.Cut(digest, ":")
	if !ok || algorithm != "sha256" {
		return fmt.Errorf("unsupported boundary archive digest %q", digest)
	}
	actual := sha256.Sum256(data)
	if hex.EncodeToString(actual[:]) != expected {
		return errors.New("timezone boundary archive SHA-256 digest does not match the release metadata")
	}
	return nil
}

func resolveTimeZones(archive []byte, cities []*cityRecord) error {
	zipReader, err := zip.NewReader(bytes.NewReader(archive), int64(len(archive)))
	if err != nil {
		return fmt.Errorf("open timezone boundary archive: %w", err)
	}

	var geoJSONFile *zip.File
	for _, file := range zipReader.File {
		if strings.HasSuffix(strings.ToLower(file.Name), ".geojson") ||
			strings.HasSuffix(strings.ToLower(file.Name), ".json") {
			if geoJSONFile != nil {
				return errors.New("timezone boundary archive contains more than one GeoJSON file")
			}
			geoJSONFile = file
		}
	}
	if geoJSONFile == nil {
		return errors.New("timezone boundary archive contains no GeoJSON file")
	}
	if geoJSONFile.UncompressedSize64 > maxGeoJSONSize {
		return fmt.Errorf("timezone GeoJSON exceeds the %d-byte size limit", maxGeoJSONSize)
	}

	reader, err := geoJSONFile.Open()
	if err != nil {
		return fmt.Errorf("open timezone GeoJSON: %w", err)
	}
	defer reader.Close()
	if err := scanGeoJSON(reader, cities); err != nil {
		return err
	}

	for _, city := range cities {
		if city.timeZone == "" {
			return fmt.Errorf("no timezone boundary contains %s, %s at %.6f, %.6f", city.city, city.country, city.latitude, city.longitude)
		}
	}
	return nil
}

func scanGeoJSON(reader io.Reader, cities []*cityRecord) error {
	decoder := json.NewDecoder(reader)
	opening, err := decoder.Token()
	if err != nil {
		return fmt.Errorf("decode timezone GeoJSON: %w", err)
	}
	if opening != json.Delim('{') {
		return errors.New("timezone GeoJSON root is not an object")
	}

	foundFeatures := false
	for decoder.More() {
		keyToken, err := decoder.Token()
		if err != nil {
			return fmt.Errorf("decode timezone GeoJSON property: %w", err)
		}
		key, ok := keyToken.(string)
		if !ok {
			return errors.New("timezone GeoJSON contains a non-string property name")
		}
		if key != "features" {
			var ignored json.RawMessage
			if err := decoder.Decode(&ignored); err != nil {
				return fmt.Errorf("decode timezone GeoJSON %q property: %w", key, err)
			}
			continue
		}

		foundFeatures = true
		opening, err := decoder.Token()
		if err != nil {
			return fmt.Errorf("decode timezone GeoJSON features: %w", err)
		}
		if opening != json.Delim('[') {
			return errors.New("timezone GeoJSON features property is not an array")
		}
		for decoder.More() {
			var feature geoJSONFeature
			if err := decoder.Decode(&feature); err != nil {
				return fmt.Errorf("decode timezone GeoJSON feature: %w", err)
			}
			if err := applyFeature(feature, cities); err != nil {
				return err
			}
		}
		if _, err := decoder.Token(); err != nil {
			return fmt.Errorf("decode end of timezone GeoJSON features: %w", err)
		}
	}
	if !foundFeatures {
		return errors.New("timezone GeoJSON contains no features property")
	}
	return nil
}

func applyFeature(feature geoJSONFeature, cities []*cityRecord) error {
	if feature.Properties.TimeZone == "" {
		return errors.New("timezone GeoJSON feature has no tzid")
	}

	switch feature.Geometry.Type {
	case "Polygon":
		var shape polygon
		if err := json.Unmarshal(feature.Geometry.Coordinates, &shape); err != nil {
			return fmt.Errorf("decode %s polygon: %w", feature.Properties.TimeZone, err)
		}
		applyPolygon(shape, feature.Properties.TimeZone, cities)
	case "MultiPolygon":
		var shapes []polygon
		if err := json.Unmarshal(feature.Geometry.Coordinates, &shapes); err != nil {
			return fmt.Errorf("decode %s multipolygon: %w", feature.Properties.TimeZone, err)
		}
		for _, shape := range shapes {
			applyPolygon(shape, feature.Properties.TimeZone, cities)
		}
	default:
		return fmt.Errorf("timezone %s uses unsupported GeoJSON geometry %q", feature.Properties.TimeZone, feature.Geometry.Type)
	}
	return nil
}

func applyPolygon(shape polygon, timeZone string, cities []*cityRecord) {
	if len(shape) == 0 || len(shape[0]) < 3 {
		return
	}
	minLongitude, maxLongitude, minLatitude, maxLatitude := bounds(shape[0])
	for _, city := range cities {
		if city.timeZone != "" ||
			city.longitude < minLongitude || city.longitude > maxLongitude ||
			city.latitude < minLatitude || city.latitude > maxLatitude ||
			!pointInRing(city.longitude, city.latitude, shape[0]) {
			continue
		}

		insideHole := false
		for _, hole := range shape[1:] {
			if pointInRing(city.longitude, city.latitude, hole) {
				insideHole = true
				break
			}
		}
		if !insideHole {
			city.timeZone = timeZone
		}
	}
}

func bounds(points ring) (float64, float64, float64, float64) {
	minLongitude, maxLongitude := points[0][0], points[0][0]
	minLatitude, maxLatitude := points[0][1], points[0][1]
	for _, point := range points[1:] {
		if point[0] < minLongitude {
			minLongitude = point[0]
		}
		if point[0] > maxLongitude {
			maxLongitude = point[0]
		}
		if point[1] < minLatitude {
			minLatitude = point[1]
		}
		if point[1] > maxLatitude {
			maxLatitude = point[1]
		}
	}
	return minLongitude, maxLongitude, minLatitude, maxLatitude
}

func pointInRing(longitude, latitude float64, points ring) bool {
	inside := false
	previous := points[len(points)-1]
	for _, current := range points {
		if (current[1] > latitude) != (previous[1] > latitude) &&
			longitude < (previous[0]-current[0])*(latitude-current[1])/(previous[1]-current[1])+current[0] {
			inside = !inside
		}
		previous = current
	}
	return inside
}

func generateKotlin(cities []*cityRecord, ianaVersion, boundaryVersion string) string {
	var kotlinMap strings.Builder
	kotlinMap.WriteString("package xyz.d1n0.lib.constant\n\n")
	kotlinMap.WriteString("import xyz.d1n0.lib.model.WorldTimezone\n\n")
	fmt.Fprintf(
		&kotlinMap,
		"// IANA timezone data %s; timezone-boundary-builder %s.\n",
		ianaVersion,
		boundaryVersion,
	)
	kotlinMap.WriteString("val WorldTimezoneData: Map<Int, WorldTimezone> = mapOf(\n")

	for _, city := range cities {
		fmt.Fprintf(
			&kotlinMap,
			"\t%d to WorldTimezone(\n\t\ttimeZone = \"%s\",\n\t\tcountry = \"%s\",\n\t\tcity = \"%s\",\n\t\tcityName = \"%s\",\n\t\tcityCode = %s,\n\t\tidentifier = %d,\n\t\toffset = %s,\n\t\tdstDiff = %s,\n\t\tdstRules = %d,\n\t\tlongitude = %.5f,\n\t\tlatitude = %.5f\n\t),\n",
			city.id,
			kotlinString(city.timeZone),
			kotlinString(city.country),
			kotlinString(city.city),
			kotlinString(city.cityName),
			kotlinNullableString(city.cityCode),
			city.id,
			kotlinDouble(city.offset),
			kotlinDouble(city.dstDiff),
			city.dstRule,
			city.longitude,
			city.latitude,
		)
	}

	return strings.TrimSuffix(kotlinMap.String(), ",\n") + "\n)"
}

// assignCityCodes gives each city the IATA city code of the nearest city with the same name
// (its city or its watch name) within maxCityCodeDistanceKm, or null if there is none.
func assignCityCodes(cities []*cityRecord, iataCities []*iataCity) {
	var missing []string
	for _, city := range cities {
		nearest := maxCityCodeDistanceKm + 1.0
		for _, iata := range iataCities {
			if !iata.hasName(city.city) && !iata.hasName(city.cityName) {
				continue
			}
			if distance := distanceKm(city.latitude, city.longitude, iata.latitude, iata.longitude); distance < nearest {
				nearest = distance
				city.cityCode = iata.code
			}
		}
		if city.cityCode == "" {
			missing = append(missing, fmt.Sprintf("%d %s, %s", city.id, city.city, city.country))
		}
	}
	printMissingCityCodes(missing, len(cities))
}

// distanceKm is the great-circle distance between two coordinates.
func distanceKm(latitudeA, longitudeA, latitudeB, longitudeB float64) float64 {
	const earthRadiusKm = 6371
	radians := func(degrees float64) float64 { return degrees * math.Pi / 180 }
	deltaLatitude := radians(latitudeB - latitudeA)
	deltaLongitude := radians(longitudeB - longitudeA)
	h := math.Pow(math.Sin(deltaLatitude/2), 2) +
		math.Cos(radians(latitudeA))*math.Cos(radians(latitudeB))*math.Pow(math.Sin(deltaLongitude/2), 2)
	return 2 * earthRadiusKm * math.Asin(math.Sqrt(h))
}

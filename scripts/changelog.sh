#!/usr/bin/env bash
# Writes the changelog for the current versionCode from feat and fix commits since the previous
# release. F-Droid reads it from the tagged commit and CI uploads it to Play, so run it after
# bumping the version and commit it with the release, before tagging.
set -euo pipefail
cd "$(dirname "$0")/.."

name=$(grep -oP 'versionName = "\K[^"]+' composeApp/build.gradle.kts)
code=$(grep -oP 'versionCode = \K[0-9]+' composeApp/build.gradle.kts)
out=fastlane/metadata/android/en-US/changelogs/$code.txt

# Production notes cover everything since the last production release, skipping RCs
exclude=$([[ "$name" == *-rc* ]] || echo '--exclude=*-rc*')
prev=$(git describe --tags --abbrev=0 $exclude HEAD 2>/dev/null || true)

mkdir -p "$(dirname "$out")"
subjects=$(git log --no-merges --format='%s' "${prev:+$prev..}HEAD")
# Features before fixes, each newest first, so the 500 character cut drops fixes first
{
	grep -E '^feat(\(.+\))?!?: ' <<<"$subjects" || true
	grep -E '^fix(\(.+\))?!?: ' <<<"$subjects" || true
} \
	| sed -E 's/^(feat|fix)(\(.+\))?!?: (.)/- \u\3/' \
	| awk '{ n += length($0) + 1; if (n > 500) exit; print }' \
	> "$out"
[ -s "$out" ] || echo "- Bug fixes and improvements" > "$out"

echo "$out (since ${prev:-the first commit}):"
cat "$out"

#!/usr/bin/env bash
# Writes the store changelog for the current versionCode: each feat commit since the previous
# release, then a single line for the fixes if there are any. F-Droid reads it from the tagged
# commit and CI uploads it to Play, so run it after bumping the version and commit it with the
# release, before tagging. The GitHub release notes come from github-release-notes.sh instead.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/release-range.sh

out=fastlane/metadata/android/en-US/changelogs/$code.txt
mkdir -p "$(dirname "$out")"

feats=$(grep -E '^\S+ feat(\(.+\))?!?: ' <<<"$subjects" | sed -E 's/^\S+ feat(\(.+\))?!?: (.)/- \u\2/' || true)
fixes=$(grep -cE '^\S+ fix(\(.+\))?!?: ' <<<"$subjects" || true)

fix_line="- Bug fixes"
[ -n "$feats" ] || fix_line="- Bug fixes and improvements"
# Play allows 500 characters, keep room for the fix line so a long feature list cuts features
budget=500
[ "$fixes" -eq 0 ] || budget=$((budget - ${#fix_line} - 1))
{
	[ -z "$feats" ] || awk -v max="$budget" '{ n += length($0) + 1; if (n > max) exit; print }' <<<"$feats"
	[ "$fixes" -eq 0 ] || echo "$fix_line"
} > "$out"
[ -s "$out" ] || echo "- Bug fixes and improvements" > "$out"

echo "$out (since ${prev:-the first commit}):"
cat "$out"

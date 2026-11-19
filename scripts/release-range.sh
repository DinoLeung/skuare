# Sourced by store-changelog.sh and github-release-notes.sh so both cover the same commits.
# Sets name and code from composeApp/build.gradle.kts, prev to the previous release tag (empty
# before the first one) and subjects to "<hash> <subject>" lines since prev, newest first.

name=$(grep -oP 'versionName = "\K[^"]+' composeApp/build.gradle.kts)
code=$(grep -oP 'versionCode = \K[0-9]+' composeApp/build.gradle.kts)

# Production notes cover everything since the last production release, skipping RCs. The current
# version's own tag is skipped too, so this gives the same answer before and after tagging.
exclude=(--exclude="$name")
[[ "$name" == *-rc* ]] || exclude+=(--exclude='*-rc*')
prev=$(git describe --tags --abbrev=0 "${exclude[@]}" HEAD 2>/dev/null || true)

subjects=$(git log --no-merges --format='%h %s' "${prev:+$prev..}HEAD")

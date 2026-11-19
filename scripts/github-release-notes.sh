#!/usr/bin/env bash
# Prints the GitHub release notes for the current version as Markdown: feat, fix and refactor
# commits since the previous release, grouped by type, newest first. CI runs it when creating the
# release; run it locally to preview. Needs the full history and tags (fetch-depth: 0).
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/release-range.sh

# section TYPE TITLE: a heading and one bullet per commit of that type, nothing if there are none
section() {
	local lines
	lines=$(grep -E "^\S+ $1(\(.+\))?!?: " <<<"$subjects" || true)
	[ -n "$lines" ] || return 0
	printf '## %s\n\n' "$2"
	# "abc1234 feat(scope)!: thing" -> "- **scope:** Thing (abc1234)", GitHub links the hash
	sed -E \
		-e "s/^(\S+) $1(\((.+)\))?(!?): (.)(.*)$/- **\3:** \u\5\6 (\1)\4/" \
		-e 's/^- \*\*:\*\* /- /' \
		-e 's/ \((\S+)\)!$/ **(breaking)** (\1)/' \
		<<<"$lines"
	printf '\n'
}

section feat Features
section fix Fixes
section refactor Refactoring

if [ -n "$prev" ] && [ -n "${GITHUB_REPOSITORY:-}" ]; then
	echo "**Full changelog**: https://github.com/$GITHUB_REPOSITORY/compare/$prev...$name"
fi

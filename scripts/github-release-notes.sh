#!/usr/bin/env bash
# Prints the GitHub release notes for the current version as Markdown: feat, fix and refactor
# commits since the previous release, grouped by type, newest first. CI runs it when creating the
# release; run it locally to preview. Needs the full history and tags (fetch-depth: 0).
# With GITHUB_REPOSITORY set (CI does) it also links the compare view and, through the gh CLI,
# credits the author of each commit that came from someone else's pull request.
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/release-range.sh

repo=${GITHUB_REPOSITORY:-}
owner=${repo%%/*}
credits=$([ -n "$repo" ] && command -v gh >/dev/null && echo yes || true)

# credit: appends " by @login" before the hash of lines whose commit came from a pull request
# opened by someone other than the repo owner, bots excluded
credit() {
	local line sha login
	while IFS= read -r line; do
		if [ -n "$credits" ]; then
			sha=${line##*(}
			sha=$(git rev-parse "${sha%)}")
			login=$(gh api "repos/$repo/commits/$sha/pulls" --jq '.[0].user.login // empty' || true)
			if [ -n "$login" ] && [ "${login,,}" != "${owner,,}" ] && [[ "$login" != *"[bot]" ]]; then
				line="${line% (*} by @$login (${line##* (}"
			fi
		fi
		printf '%s\n' "$line"
	done
}

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
		<<<"$lines" | credit
	printf '\n'
}

section feat Features
section fix Fixes
section refactor Refactoring

if [ -n "$prev" ] && [ -n "$repo" ]; then
	echo "**Full changelog**: https://github.com/$repo/compare/$prev...$name"
fi

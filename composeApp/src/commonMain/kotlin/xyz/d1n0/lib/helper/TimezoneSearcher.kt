package xyz.d1n0.lib.helper

import xyz.d1n0.lib.model.HomeTimezone
import xyz.d1n0.lib.model.Timezone
import xyz.d1n0.lib.model.WorldTimezone
import kotlin.math.abs
import kotlin.math.roundToInt

fun <T : Timezone> searchTimeZones(
	timeZones: Collection<T>,
	query: String,
): List<T> {
	query.toUtcOffsetQuery()?.let { offsetQuery ->
		return timeZones.filter { offsetQuery.matches(it.offset) }
	}

	val normalizedQuery = query.normalizeForSearch()
	if (normalizedQuery.isEmpty()) return timeZones.toList()

	val queryTokens = normalizedQuery.split(' ')
	return timeZones.mapIndexedNotNull { index, timeZone ->
		val terms = timeZone.searchTerms
		val words = terms.flatMap { it.split(' ') }.distinct()
		val tokenScores = queryTokens.map { token ->
			bestTokenScore(token, terms, words) ?: return@mapIndexedNotNull null
		}
		val phraseScore = terms.minOfOrNull { term ->
			when {
				term == normalizedQuery -> 0
				term.startsWith(normalizedQuery) -> 2
				term.contains(normalizedQuery) -> 4
				else -> 8
			}
		} ?: 8
		ScoredTimeZone(timeZone, tokenScores.sum() + phraseScore, index)
	}.sortedWith(compareBy<ScoredTimeZone<T>> { it.score }.thenBy { it.originalIndex })
		.map { it.timeZone }
}

private data class UtcOffsetQuery(
	val sign: Int?,
	val hours: Int,
	val minutes: Int?,
) {
	fun matches(offset: Double): Boolean {
		val totalMinutes = (offset * 60).roundToInt()
		when (sign) {
			1 -> if (totalMinutes < 0) return false
			-1 -> if (totalMinutes >= 0) return false
		}

		val absoluteMinutes = abs(totalMinutes)
		return if (minutes == null) {
			absoluteMinutes / 60 == hours
		} else {
			absoluteMinutes == hours * 60 + minutes
		}
	}
}

private val utcOffsetPattern =
	Regex("""^([+-])?\s*(\d{1,2})(?:[.:]([0-5]\d))?$""")

private fun String.toUtcOffsetQuery(): UtcOffsetQuery? {
	val match = utcOffsetPattern.matchEntire(trim()) ?: return null
	val sign = when (match.groupValues[1]) {
		"+" -> 1
		"-" -> -1
		else -> null
	}
	val hours = match.groupValues[2].toIntOrNull()?.takeIf { it <= 23 } ?: return null
	val minutes = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()
	return UtcOffsetQuery(sign = sign, hours = hours, minutes = minutes)
}

private data class ScoredTimeZone<T : Timezone>(
	val timeZone: T,
	val score: Int,
	val originalIndex: Int,
)

private val Timezone.searchTerms: List<String>
	get() = buildList {
		add(cityName.normalizeForSearch())
		when (this@searchTerms) {
			is HomeTimezone -> add(timeZone.normalizeForSearch())
			is WorldTimezone -> {
				add(city.normalizeForSearch())
				add(country.normalizeForSearch())
			}
		}
	}.filter { it.isNotEmpty() }.distinct()

private fun bestTokenScore(
	token: String,
	terms: List<String>,
	words: List<String>,
): Int? {
	if (words.any { it == token }) return 0
	if (words.any { it.startsWith(token) }) return 4
	if (terms.any { it.contains(token) }) return 8

	val editDistance = words.minOfOrNull { restrictedDamerauLevenshtein(token, it) } ?: return null
	val maximumDistance = when {
		token.length <= 3 -> 1
		token.length <= 7 -> 2
		else -> 3
	}
	return if (editDistance <= maximumDistance) 16 + editDistance else null
}

private fun String.normalizeForSearch(): String = lowercase()
	.map { character ->
		when {
			character.isLetterOrDigit() -> character
			else -> ' '
		}
	}.joinToString("")
	.trim()
	.replace(Regex("\\s+"), " ")

/** Optimal-string-alignment distance, including adjacent character transpositions. */
private fun restrictedDamerauLevenshtein(first: String, second: String): Int {
	if (first == second) return 0
	if (first.isEmpty()) return second.length
	if (second.isEmpty()) return first.length

	val distances = Array(first.length + 1) { IntArray(second.length + 1) }
	for (firstIndex in 0..first.length) distances[firstIndex][0] = firstIndex
	for (secondIndex in 0..second.length) distances[0][secondIndex] = secondIndex

	for (firstIndex in 1..first.length) {
		for (secondIndex in 1..second.length) {
			val substitutionCost = if (first[firstIndex - 1] == second[secondIndex - 1]) 0 else 1
			distances[firstIndex][secondIndex] = minOf(
				distances[firstIndex - 1][secondIndex] + 1,
				distances[firstIndex][secondIndex - 1] + 1,
				distances[firstIndex - 1][secondIndex - 1] + substitutionCost,
			)
			if (
				firstIndex > 1 && secondIndex > 1 &&
				first[firstIndex - 1] == second[secondIndex - 2] &&
				first[firstIndex - 2] == second[secondIndex - 1]
			) {
				distances[firstIndex][secondIndex] = minOf(
					distances[firstIndex][secondIndex],
					distances[firstIndex - 2][secondIndex - 2] + 1,
				)
			}
		}
	}
	return distances[first.length][second.length]
}

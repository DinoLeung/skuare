package xyz.d1n0.model

data class HomeTimeZone(
    val timeZone: String,
    override val cityName: String,
    override val identifier: Int,
    override val offset: Double,
    override val dstDiff: Double,
    override val dstRules: Int
) : TimeZone(cityName, identifier, offset, dstDiff, dstRules) {
    companion object {
        val homeTimeZones: Map<Int, HomeTimeZone> = mapOf(
            1 to HomeTimeZone(
                timeZone = "Australia/Adelaide",
                cityName = "Adelaide",
                identifier = 1,
                offset = 9.5,
                dstDiff = 1.0,
                dstRules = 5
            ),
            2 to HomeTimeZone(
                timeZone = "America/New_York",
                cityName = "New York",
                identifier = 2,
                offset = -5.0,
                dstDiff = 1.0,
                dstRules = 3
            )
        )

        /**
         * Perform a fuzzy search on the list of HomeTimeZones by matching the keyword with the `timeZone` field.
         * @param keyword The search keyword.
         * @return A list of HomeTimeZone objects matching the search keyword.
         */
        fun fuzzySearch(keyword: String): List<HomeTimeZone> {
            val keywordLowerCase = keyword.trim().lowercase()
            if (keywordLowerCase.isEmpty()) return homeTimeZones.values.toList()
            return homeTimeZones.values.filter { it.timeZone.lowercase().contains(keywordLowerCase) }
        }
    }
}

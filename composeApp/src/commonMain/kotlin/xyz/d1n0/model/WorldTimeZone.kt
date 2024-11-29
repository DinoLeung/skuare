package xyz.d1n0.model

data class WorldTimeZone(
    val country: String,
    val city: String,
    override val cityName: String,
    override val identifier: Int,
    override val offset: Double,
    override val dstDiff: Double,
    override val dstRules: Int
) : TimeZone(cityName, identifier, offset, dstDiff, dstRules) {
    companion object {
        val worldTimeZones: Map<Int, WorldTimeZone> = mapOf(
            2 to WorldTimeZone(
                country = "Australia",
                city = "Adealide",
                cityName = "Adelaide",
                identifier = 2,
                offset = 9.5,
                dstDiff = 1.0,
                dstRules = 5
            ),
            1 to WorldTimeZone(
                country = "America",
                city = "New York",
                cityName = "New York",
                identifier = 1,
                offset = -5.0,
                dstDiff = 1.0,
                dstRules = 3
            ),
        )

        fun fuzzySearch(keyword: String): List<WorldTimeZone> {
            val keywordLowerCase = keyword.trim().lowercase()
            if (keywordLowerCase.isEmpty()) return worldTimeZones.values.toList()
            return worldTimeZones.values.filter {
                it.city.lowercase().contains(keywordLowerCase) || it.country.lowercase().contains(keywordLowerCase)
            }
        }
    }
}

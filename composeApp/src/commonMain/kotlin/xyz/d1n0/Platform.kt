package xyz.d1n0

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
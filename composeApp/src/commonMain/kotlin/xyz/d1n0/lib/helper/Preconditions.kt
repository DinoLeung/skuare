package xyz.d1n0.lib.helper

inline fun Int.requireIn(range: IntRange, lazyMsg: () -> Any): Int =
	if (this in range) this else throw IllegalArgumentException(lazyMsg().toString())
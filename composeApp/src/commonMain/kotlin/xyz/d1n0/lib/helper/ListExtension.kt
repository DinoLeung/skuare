package xyz.d1n0.lib.helper

fun <T> List<T>.replaceAt(index: Int, with: T): List<T> =
	mapIndexed { i, old -> if (i == index) with else old }

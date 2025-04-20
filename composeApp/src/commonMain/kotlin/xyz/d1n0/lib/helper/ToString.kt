package xyz.d1n0.lib.helper

fun Double.toSignedString(): String = when {
    this == 0.0 -> "±${this.toString()}"
    this > 0.0 -> "+${this.toString()}"
    else -> this.toString()
}
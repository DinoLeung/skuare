package xyz.d1n0.lib.model

/** The watch didn't send what was asked for in time, e.g. its clocks. */
class WatchTimeoutException(message: String) : IllegalStateException(message)

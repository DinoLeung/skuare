package xyz.d1n0

import co.touchlab.kermit.*

object Log : Logger(
    config = loggerConfigInit(
        platformLogWriter(NoTagFormatter),
        minSeverity = Severity.Verbose,
    ),
    tag = "Skuare",
)
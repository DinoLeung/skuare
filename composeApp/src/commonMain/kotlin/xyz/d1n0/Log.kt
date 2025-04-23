package xyz.d1n0

import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

object Log : Logger(
	config = loggerConfigInit(
		platformLogWriter(NoTagFormatter),
		minSeverity = Severity.Verbose,
	),
	tag = "Skuare",
)

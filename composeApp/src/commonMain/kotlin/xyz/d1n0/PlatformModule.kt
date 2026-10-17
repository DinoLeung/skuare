package xyz.d1n0

import org.koin.core.module.Module

/**
 * Platform specific dependencies, needed by [coreModule]:
 * - `RoomDatabase.Builder<SkuareDatabase>`
 * - [xyz.d1n0.sync.WatchPresenceMonitor]
 */
expect val platformModule: Module

package xyz.d1n0

import org.koin.core.module.Module

/**
 * Platform specific dependencies, needed by [coreModule]:
 * - `RoomDatabase.Builder<SkuareDatabase>`
 * - [xyz.d1n0.sync.WatchPresenceMonitor]
 * - [xyz.d1n0.phone.LocationProvider]
 * - [xyz.d1n0.phone.MapLinkBuilder]
 * - [xyz.d1n0.phone.PhoneRinger]
 */
expect val platformModule: Module

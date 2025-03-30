# Skuare

<img src="skuare.svg" alt="drawing" style="width:250px;"/>

An alternative app that connects G-Shock watches via BLE.

## Background

In October 2024, a significant [data breach](https://world.casio.com/news/2025/0107-incident) occurred,
compromising the personal information of thousands of individuals, including employees, business partners, and customers.
This incident raised concerns about how user data is handled, especially considering that the official app stores
geolocation data online. 

As a long-time enthusiast of Casio watches and a proponent of user privacy, I was motivated to develop Skuare—a simple,
basic app with full support for the G-Shock Bluetooth protocol.
Skuare enables users to connect to their Casio watches with trust,
operating entirely offline to ensure that no personal data is transmitted over the internet.

The underlying protocol was reverse-engineered from the official Casio app, with significant assistance from
[GShockAPI](https://github.com/izivkov/GShockAPI/tree/main).

Currently, Skuare supports the GMW-B5000 and GW-B5600 models, as these are the only watch models I have access to.

## Why rebuild everything when [GShockAPI](https://github.com/izivkov/GShockAPI/tree/main) already exists?

[GShockAPI](https://github.com/izivkov/GShockAPI/tree/main) offers broader support for multiple Casio watch models.
However, I wanted a structure tailored more closely to my own preferences and found rebuilding from scratch to be an
excellent opportunity to deepen my understanding of Kotlin, JetBrains Compose Multiplatform, and core BLE communication concepts.

## Supported watch features

- [x] Watch settings
  - [x] Watch name
  - [ ] Battery level 
  - [x] BLE connection timeout
  - [x] Time adjustment settings
  - [x] Date time display preference
  - [x] Power saving mode
  - [x] Backlight duration
  - [x] Auto backlight
- [x] Time adjustment procedure
- [x] Home timezone and world timezone
  - [x] Timezone names, info and DST settings
    - Currently ignores outputs from the watch, it always use values from timezones lookup tables.
  - [x] Timezone gps coordinates and radio signal ID
    - For home time timezones, it tries to get coordinates from the equivalent world timezone.  
- [x] Alarms
  - [x] Hourly Signal
- [x] Timer
- [x] Reminders title and config

## Roadmap

- [x] Map out the data models
- [X] Implement BLE communication protocol
- [ ] Simple UI to pair and interact with the watch
- [ ] Local sqlite to store device hash to support auto sync procedure
- [ ] Background process to support auto time adjustment procedure
- [ ] Figure out unknown packets

## To be fixed

- [ ] Handle multiple peripheral found when scanning for watches
- [ ] Default data when invalid data received from watch, including timezone

This is a Kotlin Multiplatform project targeting Android, iOS.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

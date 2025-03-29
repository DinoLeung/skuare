# Skuare

<img src="skuare.svg" alt="drawing" style="width:250px;"/>

An alternative app that connects G-Shock watches via BLE.

## Background

In October 2024, a significant [data breach](https://world.casio.com/news/2025/0107-incident) occurred,
compromising the personal information of thousands of individuals, including employees, business partners, and customers.
This incident raised concerns about how user data is handled, especially considering that the official app stores geolocation data online. 

As a long-time enthusiast of Casio watches and a proponent of user privacy, I was motivated to develop Skuare—a simple,
basic app with full support for the G-Shock Bluetooth protocol.
Skuare enables users to connect to their Casio watches with trust,
operating entirely offline to ensure that no personal data is transmitted over the internet.

The underlying protocol was reverse-engineered from the official Casio app, with significant assistance from [GShockAPI](https://github.com/izivkov/GShockAPI/tree/main).

Currently, Skuare supports the GMW-B5000 and GW-B5600 models, as these are the only watch models I have access to.

## Why rebuilding everything knowing [GShockAPI](https://github.com/izivkov/GShockAPI/tree/main) exists?

[GShockAPI](https://github.com/izivkov/GShockAPI/tree/main) offers broader support for multiple Casio watch models.
However, I found the existing codebase difficult to navigate and wanted a more approachable, maintainable structure
tailored to my preferences.
Additionally, rebuilding from scratch provided an excellent opportunity to deepen my understanding of Kotlin, JetBrain's
Compose Multiplatform, and core BLE communication concepts.

## Supported watch features

- [x] Watch settings
  - [x] Watch name
  - [x] BLE connection timeout
  - [x] Time adjustment settings
  - [x] Backlight duration
  - [x] Date time display preference
- [x] Time adjustment procedure
- [x] Home timezone and world timezone
  - [x] Timezone DST settings 
  - [ ] Timezone gps coordinates and radio signal ID
- [x] Alarms
  - [x] Hourly Signal
- [x] Timer
- [ ] Reminders


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

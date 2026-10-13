rootProject.name = "skuare"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
	val foojayResolverVersion = providers.gradleProperty("foojayResolverVersion").get()

	plugins {
		id("org.gradle.toolchains.foojay-resolver-convention") version foojayResolverVersion
	}

	repositories {
		google {
			mavenContent {
				includeGroupAndSubgroups("androidx")
				includeGroupAndSubgroups("com.android")
				includeGroupAndSubgroups("com.google")
			}
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention")
}

dependencyResolutionManagement {
	repositories {
		google {
			mavenContent {
				includeGroupAndSubgroups("androidx")
				includeGroupAndSubgroups("com.android")
				includeGroupAndSubgroups("com.google")
			}
		}
		mavenCentral()
	}
}

include(":composeApp")

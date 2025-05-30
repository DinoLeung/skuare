import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
}

kotlin {
	androidTarget {
		@OptIn(ExperimentalKotlinGradlePluginApi::class) compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
		}
	}

	listOf(
		iosX64(), iosArm64(), iosSimulatorArm64()
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "ComposeApp"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.material)
			implementation(compose.material3)
			implementation(compose.materialIconsExtended)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation(compose.components.uiToolingPreview)
			implementation(libs.androidx.lifecycle.viewmodel)
			implementation(libs.androidx.lifecycle.runtime.compose)
			implementation(libs.androidx.lifecycle.viewmodel.compose)
			implementation(libs.androidx.navigation.common)
			implementation(libs.androidx.navigation.compose)
			implementation(libs.kotlinx.coroutines)
			implementation(libs.kotlinx.datetime)

			implementation(libs.material.icons)

			implementation(libs.kermit)
			implementation(libs.kermit.koin)

			implementation(libs.kable)

			api(libs.koin.core)
			implementation(libs.koin.compose)
			implementation(libs.koin.compose.viewmodel.navigation)

			api(libs.moko.permissions.core)
			api(libs.moko.permissions.compose)
			implementation(libs.moko.permissions.bluetooth)
			implementation(libs.moko.permissions.location)
		}

		commonTest.dependencies {
			implementation(libs.kotlin.test)
		}

//        listOf(
//            macosX64(),
//            macosArm64(),
//        ).forEach { target ->
//            target.binaries.executable {
//                baseName = "skuare"
//                entryPoint = "xyz.d1n0"
//            }
//        }

		androidMain.dependencies {
			implementation(compose.preview)
			implementation(libs.androidx.activity.compose)
		}

		iosMain.dependencies {}
	}
}

android {
	namespace = "xyz.d1n0"
	compileSdk = libs.versions.android.compileSdk.get().toInt()

	defaultConfig {
		applicationId = "xyz.d1n0"
		minSdk = libs.versions.android.minSdk.get().toInt()
		targetSdk = libs.versions.android.targetSdk.get().toInt()
		versionCode = 1
		versionName = "1.0"
	}
	packaging {
		resources {
			excludes += "/META-INF/{AL2.0,LGPL2.1}"
		}
	}
	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
		}
	}
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_11
		targetCompatibility = JavaVersion.VERSION_11
	}
}

dependencies {
	debugImplementation(compose.uiTooling)
}

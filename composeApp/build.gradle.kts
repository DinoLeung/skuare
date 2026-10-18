import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	alias(libs.plugins.kotlinMultiplatform)
	alias(libs.plugins.androidApplication)
	alias(libs.plugins.composeMultiplatform)
	alias(libs.plugins.composeCompiler)
	alias(libs.plugins.ksp)
	alias(libs.plugins.room)
}

kotlin {
	compilerOptions {
		// Room generates an actual object for the expect SkuareDatabaseConstructor
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}

	androidTarget {
		@OptIn(ExperimentalKotlinGradlePluginApi::class) compilerOptions {
			jvmTarget.set(JvmTarget.JVM_11)
		}
	}

	listOf(
		iosArm64(), iosSimulatorArm64()
	).forEach { iosTarget ->
		iosTarget.binaries.framework {
			baseName = "ComposeApp"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(libs.compose.runtime)
			implementation(libs.compose.foundation)
			implementation(libs.compose.material3)
			implementation(libs.material.icons.extended)
			implementation(libs.compose.ui)
			implementation(libs.compose.components.resources)
			implementation(libs.compose.ui.tooling.preview)
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
			implementation(libs.moko.permissions.notifications)

			implementation(libs.room.runtime)
			implementation(libs.sqlite.bundled)
		}

		commonTest.dependencies {
			implementation(libs.kotlin.test)
			implementation(libs.kotlinx.coroutines.test)
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
			implementation(libs.androidx.activity.compose)
			implementation(libs.koin.android)
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
	debugImplementation(libs.compose.ui.tooling)

	listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach {
		add(it, libs.room.compiler)
	}
}

room {
	schemaDirectory("$projectDir/schemas")
}

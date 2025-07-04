@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
	kotlin("multiplatform") version "2.2.0"
	kotlin("plugin.serialization") version "2.2.0"
}

kotlin {
	jvmToolchain(17)

	jvm {
		binaries {
			executable {
				mainClass = "MainKt"
			}
		}
	}

	sourceSets {
		commonMain {
			dependencies {
				implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
			}
		}
	}
}
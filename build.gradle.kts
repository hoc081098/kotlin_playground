import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.20-RC"
}

group = "com.hoc.kotlin_playground"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
  implementation("io.github.hoc081098:FlowExt:0.5.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.0-Beta")
  implementation("io.github.hoc081098:kmp-viewmodel:0.2.0")
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
    freeCompilerArgs = freeCompilerArgs + arrayOf(
      "-XXLanguage:+RangeUntilOperator",
      "-Xcontext-receivers"
    )
    languageVersion = "1.9"
  }
}

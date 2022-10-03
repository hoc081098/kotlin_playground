import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.20"
}

group = "com.hoc.kotlin_playground"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
  implementation("io.github.hoc081098:FlowExt:0.5.0-SNAPSHOT")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
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
    languageVersion = "1.8"
  }
}

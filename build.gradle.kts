import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.21"
}

group = "com.hoc.kotlin_playground"
version = "1.0-SNAPSHOT"

repositories {
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
  mavenCentral()
}

configurations.all {
  // Check for updates every build
  resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

dependencies {
  testImplementation(kotlin("test"))
  implementation("io.github.hoc081098:FlowExt:0.6.0"){
    isChanging = true
  }
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.0-Beta")
  implementation("io.github.hoc081098:kmp-viewmodel:0.3.1-SNAPSHOT") {
    isChanging = true
  }
  implementation("io.reactivex.rxjava3:rxjava:3.1.6")
  implementation("com.github.akarnokd:kotlin-flow-extensions:0.0.14")
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

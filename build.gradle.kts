plugins {
  val kotlinVersion = "2.0.21"
  kotlin("jvm") version kotlinVersion
  id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
  id("org.jetbrains.compose") version "1.7.0"
}

group = "com.hoc.kotlin_playground"
version = "1.0-SNAPSHOT"

repositories {
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
  mavenCentral()
  google()
}

configurations.all {
  resolutionStrategy.eachDependency { ->
    if (requested.group == "io.github.hoc081098") {
      // Check for updates every build
      resolutionStrategy.cacheChangingModulesFor(30, TimeUnit.MINUTES)
    }
  }
}

composeCompiler {
  featureFlags.add(
    org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.Companion.OptimizeNonSkippingGroups,
  )
}

dependencies {
  testImplementation(kotlin("test"))

  // Kotlin Flow Extensions
  implementation("io.github.hoc081098:FlowExt:1.0.0")

  // Kotlin Multiplatform ViewModel, SavedStateHandle, Compose Multiplatform ViewModel
  val kmpViewModel = "0.8.0"
  implementation("io.github.hoc081098:kmp-viewmodel:$kmpViewModel")
  implementation("io.github.hoc081098:kmp-viewmodel-savedstate:$kmpViewModel")
  implementation("io.github.hoc081098:kmp-viewmodel-compose:$kmpViewModel")
  // Kotlin Channel Event Bus
  implementation("io.github.hoc081098:channel-event-bus:0.1.0")
  // Solivagant - Compose Multiplatform Navigation
  implementation("io.github.hoc081098:solivagant-navigation:0.5.0")

  // Compose
  implementation(compose.runtime)
  implementation(compose.foundation)
  implementation(compose.material3)
  implementation(compose.materialIconsExtended)
  implementation(compose.desktop.currentOs)

  // Coroutines
  val coroutines = "1.9.0"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutines")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")

  // RxJava3 and Kotlin Flow Extensions
  implementation("io.reactivex.rxjava3:rxjava:3.1.9")
  implementation("com.github.akarnokd:kotlin-flow-extensions:0.0.14")

  // reactivestate
  api(platform("com.ensody.reactivestate:reactivestate-bom:5.8.1"))
  implementation("com.ensody.reactivestate:reactivestate")

  // Arrow-kt
  val arrow = "1.2.4"
  implementation("io.arrow-kt:arrow-core:$arrow")
  implementation("io.arrow-kt:arrow-fx-coroutines:$arrow")
  implementation("io.arrow-kt:arrow-autoclose:$arrow")
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    freeCompilerArgs.addAll(
      "-Xcontext-receivers",
    )
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_22
  targetCompatibility = JavaVersion.VERSION_22
}

kotlin {
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

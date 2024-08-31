import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  val kotlinVersion = "2.0.0"
  kotlin("jvm") version kotlinVersion
  id("org.jetbrains.compose") version "1.6.10"
  id("org.jetbrains.kotlin.plugin.compose") version kotlinVersion
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
  enableStrongSkippingMode = true
}

dependencies {
  testImplementation(kotlin("test"))

  implementation("io.github.hoc081098:FlowExt:1.0.0-RC")
  val kmpViewModel = "0.8.0"
  implementation("io.github.hoc081098:kmp-viewmodel:$kmpViewModel")
  implementation("io.github.hoc081098:kmp-viewmodel-savedstate:$kmpViewModel")
  implementation("io.github.hoc081098:kmp-viewmodel-compose:$kmpViewModel")
  implementation("io.github.hoc081098:channel-event-bus:0.1.0")
  implementation("io.github.hoc081098:solivagant-navigation:0.5.0")
  implementation(compose.runtime)
  implementation(compose.foundation)
  implementation(compose.material3)
  implementation(compose.materialIconsExtended)
  implementation(compose.desktop.currentOs)

  val coroutines = "1.8.1"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutines")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

  implementation("io.reactivex.rxjava3:rxjava:3.1.8")
  implementation("com.github.akarnokd:kotlin-flow-extensions:0.0.14")

  api(platform("com.ensody.reactivestate:reactivestate-bom:5.7.0"))
  implementation("com.ensody.reactivestate:reactivestate")

  val arrow = "1.2.4"
  implementation("io.arrow-kt:arrow-core:$arrow")
  implementation("io.arrow-kt:arrow-fx-coroutines:$arrow")
  implementation("io.arrow-kt:arrow-autoclose:$arrow")
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_13.toString()
    freeCompilerArgs = freeCompilerArgs + arrayOf(
      "-XXLanguage:+RangeUntilOperator",
      "-Xcontext-receivers"
    )
    compilerOptions
      .languageVersion
      .set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_13
  targetCompatibility = JavaVersion.VERSION_13
}

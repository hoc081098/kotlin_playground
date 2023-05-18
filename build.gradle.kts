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
  
  implementation("io.github.hoc081098:FlowExt:0.7.0-SNAPSHOT") {
    isChanging = true
  }
  implementation("io.github.hoc081098:kmp-viewmodel:0.3.1-SNAPSHOT") {
    isChanging = true
  }
  
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.0")
  
  implementation("io.reactivex.rxjava3:rxjava:3.1.6")
  implementation("com.github.akarnokd:kotlin-flow-extensions:0.0.14")
  
  api(platform("com.ensody.reactivestate:reactivestate-bom:5.2.1"))
  implementation("com.ensody.reactivestate:reactivestate")
  
  implementation("io.arrow-kt:arrow-core:1.2.0-RC")
  implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0-RC")
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
  }
}

tasks
  .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>()
  .configureEach {
    compilerOptions
      .languageVersion
      .set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
  }
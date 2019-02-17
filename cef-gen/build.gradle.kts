import org.jetbrains.kotlin.gradle.plugin.*
import org.jonnyzzz.cef.gradle.div

plugins {
  kotlin("jvm")
  application
}

repositories {
  mavenCentral()
}

val kotlinVersion by lazy {
  //https://youtrack.jetbrains.com/issue/KT-19788
  plugins.withType<KotlinBasePluginWrapper>().map { it.kotlinPluginVersion }.distinct().single()
}

configurations.forEach { config ->
  config.resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
      useVersion(kotlinVersion)
    }
  }
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-native-library-reader:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-native-utils:$kotlinVersion")
}

configure<ApplicationPluginConvention> {
  mainClassName = "org.jonnyzzz.cef.generator.MainKt"
}


tasks.getByName<JavaExec>("run") {
  args = listOf(
          (project(":mpp").buildDir / "classes/kotlin/macosX64/main/mpp-cinterop-kotlinCefInterop.klib").path)
}

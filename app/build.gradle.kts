plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

import java.net.URI

android {
  namespace = "com.redbolt.screenshot"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.redbolt.screenshot"
    minSdk = 26
    targetSdk = 36
    versionCode = 25
    versionName = "1.5.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debugConfig")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.savedstate)
  implementation(libs.androidx.lifecycle.service)
  testImplementation(libs.junit)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register("downloadDotoFont") {
  notCompatibleWithConfigurationCache("Requires network download")
  doLast {
    val destFile = file("src/main/res/font/doto_rounded_semibold.ttf")
    if (destFile.exists() && destFile.length() >= 1024) return@doLast
    destFile.parentFile.mkdirs()
    URI(
      "https://github.com/google/fonts/raw/main/ofl/doto/Doto%5BROND%2Cwght%5D.ttf",
    ).toURL().openStream().buffered().use { input ->
      destFile.outputStream().buffered().use { output ->
        input.copyTo(output)
      }
    }
  }
}

tasks.register("downloadRobotoMonoFont") {
  notCompatibleWithConfigurationCache("Requires network download")
  doLast {
    val destFile = file("src/main/res/font/roboto_mono.ttf")
    if (destFile.exists() && destFile.length() >= 1024) return@doLast
    val fallback = rootProject.file("../bolt-signin/app/src/main/res/font/roboto_mono.ttf")
    if (fallback.exists() && fallback.length() >= 1024) {
      destFile.parentFile.mkdirs()
      fallback.copyTo(destFile, overwrite = true)
      return@doLast
    }
    destFile.parentFile.mkdirs()
    URI(
      "https://github.com/googlefonts/RobotoMono/raw/refs/heads/main/fonts/ttf/RobotoMono-Regular.ttf",
    ).toURL().openStream().buffered().use { input ->
      destFile.outputStream().buffered().use { output ->
        input.copyTo(output)
      }
    }
  }
}

tasks.named("preBuild") {
  dependsOn("downloadDotoFont", "downloadRobotoMonoFont")
}

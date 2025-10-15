import java.util.Properties
import java.io.FileInputStream

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
  kotlin("plugin.serialization") version "1.8.22"
}
android {
  namespace = "com.maxiptv"
  compileSdk = 34
  defaultConfig {
    applicationId = "com.maxiptv"
    minSdk = 21
    targetSdk = 34
    versionCode = 11
    versionName = "1.0.10"
    vectorDrawables.useSupportLibrary = true
    buildConfigField("String", "DEFAULT_PLAYER_API", "\"https://canais.is/\"")
    buildConfigField("String", "DEFAULT_USER", "\"max\"")
    buildConfigField("String", "DEFAULT_PASS", "\"1h2yd90\"")
  }
  signingConfigs {
    create("release") {
      val keystorePropertiesFile = rootProject.file("keystore.properties")
      if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
      }
    }
  }
  
  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { 
      isMinifyEnabled = false
      applicationIdSuffix = ".debug"
    }
  }
  buildFeatures { compose = true; buildConfig = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.4.8" }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  packaging { resources.excludes += setOf("META-INF/LICENSE*", "META-INF/DEPENDENCIES") }
}
dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
  implementation(composeBom); androidTestImplementation(composeBom)
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.3.0")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.navigation:navigation-compose:2.8.0")
  implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
  implementation("androidx.tv:tv-material:1.0.0-alpha10")
  // ExoPlayer moderno (agora parte do AndroidX Media3)
  implementation("androidx.media3:media3-exoplayer:1.4.1")
  implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
  implementation("androidx.media3:media3-ui:1.4.1")
  implementation("androidx.media3:media3-common:1.4.1")
  implementation("com.squareup.okhttp3:okhttp:4.11.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

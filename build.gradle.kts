buildscript { 
  dependencies { 
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22")
    classpath("org.jetbrains.kotlin:kotlin-serialization:1.8.22")
  } 
}
plugins {
  id("com.android.application") version "8.2.2" apply false
  id("org.jetbrains.kotlin.android") version "1.8.22" apply false
  id("com.google.devtools.ksp") version "1.8.22-1.0.11" apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "1.8.22" apply false
}

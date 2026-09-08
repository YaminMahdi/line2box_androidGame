buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin") {
            version { strictly("2.4.20") }
        }
    }
}
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("androidx.room3") version "3.0.2" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
}



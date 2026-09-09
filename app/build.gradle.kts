import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.room3")
    id("com.google.devtools.ksp")
}

val secrets = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xwhen-expressions=indy",
            "-Xcontext-sensitive-resolution",
            "-Xcollection-literals"
        )
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    signingConfigs {
//         Uncomment and configure your signing config if needed
//         debug {
//             storeFile = file("/home/yamin_khan/Documents/keys/debug.keystore")
//             keyAlias = "androiddebugkey"
//             storePassword = "android"
//             keyPassword = "android"
//         }
        val release = create("release") {
            storeFile = file("C:/Documents/keys/line2box_key.jks")
            keyAlias = "key0"
            secrets.getProperty("keyPass")?.let {
                storePassword = it
                keyPassword = it
            }
        }
        getByName("debug") {
            initWith(release)
        }
    }
    namespace = "com.diu.yk_games.line2box"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.diu.yk_games.line2box"
        minSdk = 27
        targetSdk = 37
        versionCode = 17
        versionName = "1.17"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // signingConfig = signingConfigs.getByName("release")
            // minifyEnabled = false
            // shrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach {
            val output = it as com.android.build.api.variant.impl.VariantOutputImpl
            val projectName = rootProject.name.replace(" ", "_")
            val version = output.versionName.get()
            val buildType = variant.name
            output.outputFileName = "${projectName}_v$version-$buildType.apk"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom-alpha:2026.08.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.fragment:fragment-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-viewbinding:1.12.0")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.fragment:fragment-ktx:1.9.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.10.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.10.0")

    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.google.android.gms:play-services-games-v2:22.0.0")
    implementation("com.google.android.gms:play-services-auth:22.0.0")

    implementation("com.google.code.gson:gson:2.14.0")
    implementation ("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.2")
    implementation("org.jsoup:jsoup:1.23.2")

    // Room 3
    implementation("androidx.room3:room3-runtime:3.0.2")
    ksp("androidx.room3:room3-compiler:3.0.2")

    implementation("io.ak1:bubbletabbar:1.0.8")
    implementation("com.github.GwonHyeok:StickySwitch:0.0.16")

    // Dimension libraries (sdp, ssp)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom-alpha:2026.08.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Play In-App Review:
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Play In-App Update:
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Custom Tabs
    implementation("androidx.browser:browser:1.10.0")

    implementation("io.coil-kt.coil3:coil:3.6.2")
    implementation("io.coil-kt.coil3:coil-gif:3.6.2")
    implementation("io.coil-kt.coil3:coil-compose:3.6.2")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.2")

    implementation("com.github.chesire:lifecyklelog:3.1.1")
}

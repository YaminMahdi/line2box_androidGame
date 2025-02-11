plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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
//        getByName("debug") {
//            storeFile = file("C:/Documents/keys/line2box_key.jks")
//            keyAlias = "key0"
//            storePassword = "**"
//            keyPassword = "**"
//        }

    }
    namespace = "com.diu.yk_games.line2box"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diu.yk_games.line2box"
        minSdk = 27
        targetSdk = 35
        versionCode = 14
        versionName = "1.14"
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
    applicationVariants.all {
        outputs.forEach {
            val output = it as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "${rootProject.name.replace(' ', '_')}_v"
            output.outputFileName += "$versionName-$name.apk"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }
    kotlin {
        jvmToolchain(23)
        compilerOptions {
            freeCompilerArgs.addAll("-Xcontext-receivers", "-Xwhen-guards", "-Xnon-local-break-continue")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.android.gms:play-services-games-v2:20.1.2")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("com.google.code.gson:gson:2.12.1")
    implementation("org.jsoup:jsoup:1.18.3")

    implementation("io.ak1:bubbletabbar:1.0.8")
    implementation("com.github.GwonHyeok:StickySwitch:0.0.16")

    // Dimension libraries (sdp, ssp)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Play In-App Review:
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Play In-App Update:
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Custom Tabs
    implementation("androidx.browser:browser:1.8.0")

    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-gif:3.1.0")

}

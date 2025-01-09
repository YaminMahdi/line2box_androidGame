plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    signingConfigs {
        // Uncomment and configure your signing config if needed
        // debug {
        //     storeFile = file("/home/yamin_khan/Documents/keys/debug.keystore")
        //     keyAlias = "androiddebugkey"
        //     storePassword = "android"
        //     keyPassword = "android"
        // }
    }
    namespace = "com.diu.yk_games.line2box"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.diu.yk_games.line2box"
        minSdk = 27
        targetSdk = 35
        versionCode = 13
        versionName = "1.13"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            // signingConfig = signingConfigs.getByName("release")
            // minifyEnabled = false
            // shrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // debug {
        //     signingConfig = signingConfigs.getByName("debug")
        // }
    }
    applicationVariants.all {
        outputs.forEach {
            val output = it as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "${rootProject.name.replace(' ', '_')}_v"
            output.outputFileName += "$versionName-$name.apk"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
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

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.android.gms:play-services-games-v2:20.0.0")
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.17.2")

    implementation("io.ak1:bubbletabbar:1.0.8")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.28")
    implementation("com.mikhaellopez:circularimageview:4.3.1")
    implementation("com.github.GwonHyeok:StickySwitch:0.0.16")

    // Dimension libraries (sdp, ssp)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Play In-App Review:
    implementation("com.google.android.play:review:2.0.1")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Play In-App Update:
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Custom Tabs
    implementation("androidx.browser:browser:1.8.0")
}

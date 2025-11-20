plugins {
    id("com.android.application")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

val secrets = org.jetbrains.kotlin.konan.properties.loadProperties("${rootDir}/local.properties")

kotlin {
    jvmToolchain(25)
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xwhen-guards",
            "-Xnon-local-break-continue",
            "-Xcontext-sensitive-resolution",
            "-Xallow-condition-implies-returns-contracts"
        )
    }
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.diu.yk_games.line2box"
        minSdk = 27
        targetSdk = 36
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
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_25
//        targetCompatibility = JavaVersion.VERSION_25
//    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
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
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.android.gms:play-services-games-v2:21.0.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.jsoup:jsoup:1.21.2")

    implementation("io.ak1:bubbletabbar:1.0.8")
    implementation("com.github.GwonHyeok:StickySwitch:0.0.16")

    // Dimension libraries (sdp, ssp)
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // Play In-App Review:
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Play In-App Update:
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Custom Tabs
    implementation("androidx.browser:browser:1.9.0")

    implementation("io.coil-kt.coil3:coil:3.3.0")
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")

}

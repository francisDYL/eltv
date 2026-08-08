plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.megaiptv.eltv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.megaiptv.eltv"
        minSdk = 31
        targetSdk = 34
        versionCode = 9
        versionName = "1.1.7"

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
        
        // Optimize for large screens and prevent rendering issues on 4K displays
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Reduce dex overhead for large screen devices
            multiDexEnabled = false
        }
        debug {
            isMinifyEnabled = false
            multiDexEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.splashscreen)
    // UI Android TV
    implementation(libs.leanback)

    // Images — OkHttp3 integration pour charger les logos via notre client SSL
    implementation(libs.glide.core)
    implementation(libs.glide.okhttp3)
    annotationProcessor(libs.glide.compiler)

    // Base de données Room
    implementation(libs.roomRuntime)
    annotationProcessor(libs.roomCompiler)

    // Lecteur Media3/ExoPlayer — avec support OkHttp (SSL + timeout)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // Réseau OkHttp (playlists M3U + SSL étendu)
    implementation(libs.okhttp.core)
}